package com.matmode.processor_service.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.matmode.processor_service.listener.dto.PredictResponse;
import com.matmode.processor_service.listener.dto.ProcessStatusMessage;
import com.matmode.processor_service.model.MetaInf;
import com.matmode.processor_service.model.Metrics;
import com.matmode.processor_service.model.Predict;
import com.matmode.processor_service.repository.MetaInfRepository;
import com.matmode.processor_service.repository.MetricsRepository;
import com.matmode.processor_service.repository.PredictRepository;
import com.matmode.processor_service.service.dto.DataUploaded;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataUploadedListener {

    private final ObjectMapper objectMapper;
    private final MinioClient minioClient;
    private final PredictRepository predictRepository;
    private final MetricsRepository metricsRepository;
    private final MetaInfRepository metaInfRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${minio.bucket}")
    private String bucketName;

    @KafkaListener(topics = "data_uploaded", groupId = "data-uploaded")
    @Transactional
    public void handleDataUploaded(String message) {
        List<InputStream> fileStreams = new ArrayList<>();
        try {
            // Десериализация сообщения
            log.debug("Received Kafka message: {}", message);
            DataUploaded dataUploaded = objectMapper.readValue(message, DataUploaded.class);
            String batchId = dataUploaded.getBatchId();
            List<String> fileNames = dataUploaded.getFileNames();

            log.info("Received message for batchId: {}, fileNames: {}", batchId, fileNames);

            // Получение файлов из MinIO и сбор InputStream
            if (fileNames.size() != 1) {
                log.error("Expected exactly one file, but received {} for batchId: {}", fileNames.size(), batchId);
                throw new RuntimeException("Expected exactly one file for batchId: " + batchId);
            }

            String fileName = fileNames.get(0);
            try {
                InputStream inputStream = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(bucketName)
                                .object(fileName)
                                .build());
                fileStreams.add(inputStream);
                log.info("Successfully retrieved file {} from MinIO", fileName);
            } catch (Exception e) {
                log.error("Error retrieving file {} from MinIO: {}", fileName, e.getMessage(), e);
                closeStreams(fileStreams);
                throw new RuntimeException("Failed to retrieve file: " + fileName, e);
            }

            // Создание и сохранение объекта MetaInf
            MetaInf metaInf = new MetaInf();
            metaInf.setBatchId(batchId);
            metaInf.setFileNames(fileNames.toArray(new String[0]));
            metaInf.setTargetFileName("target_" + batchId + ".csv");
            metaInf.setUploadTime(LocalDateTime.now());
            try {
                metaInfRepository.save(metaInf);
                log.info("Saved MetaInf for batchId: {}", batchId);
            } catch (Exception e) {
                log.error("Error saving MetaInf for batchId {}: {}", batchId, e.getMessage(), e);
                closeStreams(fileStreams);
                throw new RuntimeException("Failed to save MetaInf", e);
            }

            // Вызов API модели с одним InputStream и batchId
            PredictResponse predictResponse = callModelApi(fileStreams.get(0), batchId);

            // Сохранение предсказаний
            if (predictResponse.getPredictions() == null || predictResponse.getPredictions().isEmpty()) {
                log.warn("No predictions received for batchId: {}", batchId);
            } else {
                List<Predict> predictions = predictResponse.getPredictions().stream()
                        .filter(Objects::nonNull)
                        .map(dto -> {
                            if (dto.getDate() == null || dto.getWarehouse() == null || dto.getStackNumber() == null ||
                                    dto.getCoalBrand() == null || dto.getFireLabel() == null ||
                                    dto.getFireProbability() == null || dto.getBatchId() == null) {
                                log.warn("Skipping invalid PredictDTO with null fields for batchId: {}", batchId);
                                return null;
                            }
                            Predict predict = new Predict();
                            predict.setDate(dto.getDate());
                            predict.setWarehouse(dto.getWarehouse());
                            predict.setStackNumber(dto.getStackNumber());
                            predict.setCoalBrand(dto.getCoalBrand());
                            predict.setFireLabel(dto.getFireLabel());
                            predict.setFireProbability(dto.getFireProbability());
                            predict.setBatchId(dto.getBatchId());
                            return predict;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                try {
                    predictRepository.saveAll(predictions);
                    log.info("Saved {} predictions for batchId: {}", predictions.size(), batchId);
                } catch (Exception e) {
                    log.error("Error saving predictions for batchId {}: {}", batchId, e.getMessage(), e);
                    closeStreams(fileStreams);
                    throw new RuntimeException("Failed to save predictions", e);
                }
            }

            // Сохранение метрик
            if (predictResponse.getMetrics() == null) {
                log.error("No metrics received for batchId: {}", batchId);
                throw new RuntimeException("Metrics are missing in API response");
            }
            Metrics metrics = new Metrics();
            metrics.setBatchId(batchId);
            if (predictResponse.getMetrics().getF1() == null || predictResponse.getMetrics().getPrecision() == null ||
                    predictResponse.getMetrics().getRecall() == null) {
                log.error("Invalid MetricsDTO with null fields for batchId: {}", batchId);
                throw new RuntimeException("Metrics contain null fields");
            }
            metrics.setF1(predictResponse.getMetrics().getF1());
            metrics.setPrecision(predictResponse.getMetrics().getPrecision());
            metrics.setRecall(predictResponse.getMetrics().getRecall());

            try {
                metricsRepository.save(metrics);
                log.info("Saved metrics for batchId: {}: f1={}, precision={}, recall={}",
                        batchId, metrics.getF1(), metrics.getPrecision(), metrics.getRecall());
            } catch (Exception e) {
                log.error("Error saving metrics for batchId {}: {}", batchId, e.getMessage(), e);
                closeStreams(fileStreams);
                throw new RuntimeException("Failed to save metrics", e);
            }

            // Отправка сообщения через WebSocket
            messagingTemplate.convertAndSend(
                    "/topic/process-status",
                    new ProcessStatusMessage(
                            "processing_completed",
                            batchId,
                            "Processing completed for batchId: " + batchId,
                            null
                    )
            );
            log.info("Sent WebSocket message for batchId: {}", batchId);

        } catch (Exception e) {
            log.error("Error processing data_uploaded message: {}", e.getMessage(), e);
            closeStreams(fileStreams);
            throw new RuntimeException("Error processing data_uploaded message", e);
        }
    }

    private PredictResponse callModelApi(InputStream fileStream, String batchId) {
        try {
            log.info("Calling model API for batchId: {}", batchId);

            // Создаем HTTP-клиент
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost("http://model-pipeline:8000/predict/");

            // Создаем MultipartEntity для отправки одного файла и batchId
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addBinaryBody("file", fileStream, ContentType.create("text/csv"), "data.csv");
            builder.addTextBody("batch_id", batchId, ContentType.TEXT_PLAIN);
            httpPost.setEntity(builder.build());

            // Выполняем запрос
            CloseableHttpResponse response = httpClient.execute(httpPost);
            try {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity());
                if (statusCode != 200) {
                    log.error("Model API returned status: {}, response: {}", statusCode, responseBody);
                    throw new RuntimeException("Model API request failed with status: " + statusCode);
                }

                // Парсим JSON в PredictResponse
                log.debug("Model API response: {}", responseBody);
                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());
                PredictResponse predictResponse = mapper.readValue(responseBody, PredictResponse.class);

                if (predictResponse.getPredictions() == null || predictResponse.getPredictions().isEmpty()) {
                    log.warn("Received empty or null predictions list for batchId: {}", batchId);
                } else {
                    log.info("Received {} predictions for batchId: {}", predictResponse.getPredictions().size(), batchId);
                }

                if (predictResponse.getMetrics() == null) {
                    log.error("No metrics received for batchId: {}", batchId);
                    throw new RuntimeException("Metrics are missing in API response");
                }

                return predictResponse;

            } finally {
                response.close();
                httpClient.close();
            }

        } catch (Exception e) {
            log.error("Error calling model API for batchId {}: {}", batchId, e.getMessage(), e);
            throw new RuntimeException("Failed to call model API", e);
        } finally {
            if (fileStream != null) {
                try {
                    fileStream.close();
                    log.info("Closed InputStream");
                } catch (Exception e) {
                    log.error("Error closing InputStream: {}", e.getMessage(), e);
                }
            }
        }
    }

    private void closeStreams(List<InputStream> streams) {
        for (InputStream stream : streams) {
            try {
                if (stream != null) {
                    stream.close();
                    log.info("Closed InputStream");
                }
            } catch (Exception e) {
                log.error("Error closing InputStream: {}", e.getMessage(), e);
            }
        }
    }
}