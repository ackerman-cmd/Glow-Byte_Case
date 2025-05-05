package com.matmode.processor_service.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matmode.processor_service.model.Fire;
import com.matmode.processor_service.model.Metrics;
import com.matmode.processor_service.model.MetricsReadyMessage;
import com.matmode.processor_service.model.Predict;
import com.matmode.processor_service.repository.FireRepository;
import com.matmode.processor_service.repository.MetricsRepository;
import com.matmode.processor_service.repository.PredictRepository;
import com.matmode.processor_service.service.dto.TargetUploaded;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import smile.validation.metric.AUC;
import smile.validation.metric.Precision;
import smile.validation.metric.Recall;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TargetUploadedListener {

    private final FireRepository fireRepository;
    private final PredictRepository predictRepository;
    private final MetricsRepository metricsRepository;
    private final MinioClient minioClient;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${minio.bucket}")
    private String bucketName;

    @Autowired
    public TargetUploadedListener(
            FireRepository fireRepository,
            PredictRepository predictRepository,
            MetricsRepository metricsRepository,
            MinioClient minioClient,
            ObjectMapper objectMapper,
            SimpMessagingTemplate messagingTemplate) {
        this.fireRepository = fireRepository;
        this.predictRepository = predictRepository;
        this.metricsRepository = metricsRepository;
        this.minioClient = minioClient;
        this.objectMapper = objectMapper;
        this.messagingTemplate = messagingTemplate;
    }

    @KafkaListener(topics = "target_uploaded", groupId = "target-uploaded")
    @Transactional
    public void handleTargetUploaded(String message) {
        try {
            // Deserialize Kafka message
            log.debug("Received Kafka message: {}", message);
            TargetUploaded targetUploaded;
            try {
                targetUploaded = objectMapper.readValue(message, TargetUploaded.class);
            } catch (Exception e) {
                log.error("Failed to deserialize Kafka message: {}", message, e);
                throw new RuntimeException("Invalid Kafka message format", e);
            }
            String batchId = targetUploaded.getBatchId();
            String fileName = targetUploaded.getFileName();

            log.info("Processing file: {} for batchId: {}", fileName, batchId);

            // Retrieve file from MinIO
            List<Fire> fires = new ArrayList<>();
            try (var inputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build());
                 var reader = new BufferedReader(new InputStreamReader(inputStream))) {

                // Parse CSV file, skipping header
                String line;
                boolean isFirstLine = true;
                int lineNumber = 0;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    if (isFirstLine) {
                        isFirstLine = false;
                        continue;
                    }
                    try {
                        Fire fire = parseCsvLineToFire(line, batchId);
                        fire.setBatchId(batchId); // Устанавливаем batchId
                        fires.add(fire);
                        log.debug("Parsed Fire entity for batchId: {}, line {}: {}", batchId, lineNumber, line);
                    } catch (Exception e) {
                        log.error("Failed to parse CSV line {} for batchId {}: {}", lineNumber, batchId, line, e);
                        throw new RuntimeException("Failed to parse CSV line " + lineNumber, e);
                    }
                }

                // Save Fire entities
                try {
                    fireRepository.saveAll(fires);
                    log.info("Saved {} Fire entities for batchId: {}", fires.size(), batchId);
                } catch (Exception e) {
                    log.error("Failed to save Fire entities for batchId {}: {}", batchId, e.getMessage(), e);
                    throw new RuntimeException("Failed to save Fire entities", e);
                }

                log.info("Completed processing file: {} for batchId: {}", fileName, batchId);

                // Calculate and save metrics
                calculateAndSaveMetrics(batchId);

                // Send WebSocket message
                messagingTemplate.convertAndSend(
                        "/topic/metrics-ready",
                        new com.matmode.processor_service.model.MetricsReadyMessage(
                                "metrics_ready",
                                batchId,
                                "Metrics calculated and saved for batchId: " + batchId,
                                null
                        )
                );
                log.info("Sent WebSocket message for metrics_ready, batchId: {}", batchId);

            } catch (Exception e) {
                log.error("Failed to retrieve or process file: {} for batchId {}: {}", fileName, batchId, e.getMessage(), e);
                throw new RuntimeException("Failed to process file: " + fileName, e);
            }

        } catch (Exception e) {
            log.error("Failed to process Kafka message: {}: {}", message, e.getMessage(), e);
            throw new RuntimeException("Error processing target_uploaded message", e);
        }
    }

    // Parse a CSV line into a Fire entity
    private Fire parseCsvLineToFire(String line, String batchId) {
        Fire fire = new Fire();

        try (CSVReader csvReader = new CSVReader(new StringReader(line))) {
            String[] columns = csvReader.readNext();
            if (columns == null || columns.length != 8) {
                log.error("Invalid CSV line for batchId: {}, expected 8 columns, got: {}", batchId, columns == null ? 0 : columns.length);
                throw new RuntimeException("Invalid CSV format: incorrect number of columns");
            }

            try {
                // Define formatters for CSV date and date-time formats
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

                // Map CSV columns to Fire fields
                fire.setReportDate(LocalDate.parse(columns[0].trim(), dateFormatter));
                fire.setCargo(columns[1].trim());
                fire.setWeight(Double.parseDouble(columns[2].trim().replace(",", ".")));
                fire.setWarehouse(Integer.parseInt(columns[3].trim()));
                fire.setStartDateTime(LocalDateTime.parse(columns[4].trim(), dateTimeFormatter));
                fire.setEndDateTime(LocalDateTime.parse(columns[5].trim(), dateTimeFormatter));
                fire.setStackFormingStart(LocalDateTime.parse(columns[6].trim(), dateTimeFormatter));
                fire.setStackNumber(Integer.parseInt(columns[7].trim()));
            } catch (DateTimeParseException e) {
                log.error("Date parsing error in CSV line: {} for batchId: {}, column: {}", line, batchId, getFailedColumn(columns, e), e);
                throw new RuntimeException("Invalid date or date-time format in CSV", e);
            } catch (NumberFormatException e) {
                log.error("Number parsing error in CSV line: {} for batchId: {}, column: {}", line, batchId, getFailedColumn(columns, e), e);
                throw new RuntimeException("Invalid number format in CSV", e);
            }
        } catch (CsvValidationException | IOException e) {
            log.error("Error reading CSV line: {} for batchId: {}", line, batchId, e);
            throw new RuntimeException("Failed to parse CSV line", e);
        }

        return fire;
    }

    // Identify the problematic column for error logging
    private String getFailedColumn(String[] columns, Exception e) {
        String failedText = e instanceof DateTimeParseException ? ((DateTimeParseException) e).getParsedString() : "unknown";
        for (int i = 0; i < columns.length; i++) {
            if (columns[i].trim().equals(failedText)) {
                return "column " + (i + 1) + " (" + columns[i] + ")";
            }
        }
        return "column unknown (" + failedText + ")";
    }

    // Calculate and save metrics based on Predict and Fire data
    private void  calculateAndSaveMetrics(String batchId) {
        try {
            // Retrieve predictions and fires by batchId
            List<Predict> predictions = predictRepository.findByBatchId(batchId);
            List<Fire> fires = fireRepository.findByBatchId(batchId);

            if (predictions.isEmpty() || fires.isEmpty()) {
                log.warn("No predictions or fires found for batchId: {}", batchId);
                throw new RuntimeException("Insufficient data to calculate metrics for batchId: " + batchId);
            }

            // Extract predicted labels and probabilities
            int[] predictedLabels = predictions.stream()
                    .mapToInt(Predict::getFireLabel)
                    .toArray();
            double[] probabilities = predictions.stream()
                    .mapToDouble(Predict::getFireProbability)
                    .toArray();

            // Extract actual labels (assumption: fire occurred if end_date_time is not null)
            int[] actualLabels = fires.stream()
                    .mapToInt(fire -> fire.getEndDateTime() != null ? 1 : 0)
                    .toArray();

            // Ensure lengths match
            if (predictedLabels.length != actualLabels.length) {
                log.error("Mismatch in data lengths for batchId: {}, predictions: {}, fires: {}",
                        batchId, predictedLabels.length, actualLabels.length);
                throw new RuntimeException("Mismatch in prediction and fire data lengths");
            }

            // Calculate metrics using SMILE
            double rocAuc = AUC.of(actualLabels, probabilities);
            double precision = Precision.of(actualLabels, predictedLabels);
            double recall = Recall.of(actualLabels, predictedLabels);

            // Create and save Metrics entity
            Metrics metrics = new Metrics();
            metrics.setBatchId(batchId);

            metrics.setPrecision(precision);
            metrics.setRecall(recall);

            try {
                metricsRepository.save(metrics);
                log.info("Saved metrics for batchId: {}: ROC AUC={}, Precision={}, Recall={}",
                        batchId, rocAuc, precision, recall);
            } catch (Exception e) {
                log.error("Failed to save metrics for batchId {}: {}", batchId, e.getMessage(), e);
                throw new RuntimeException("Failed to save metrics", e);
            }

        } catch (Exception e) {
            log.error("Failed to calculate metrics for batchId {}: {}", batchId, e.getMessage(), e);
            throw new RuntimeException("Error calculating metrics for batchId: " + batchId, e);
        }
    }
}