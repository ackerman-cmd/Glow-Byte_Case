package com.matmode.processor_service.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matmode.processor_service.model.MetaInf;
import com.matmode.processor_service.repository.MetaInfRepository;
import com.matmode.processor_service.service.dto.DataUploaded;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class DataUploadedListener {

    private final MetaInfRepository metaInfRepository;
    private final MinioClient minioClient;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Value("${minio.bucket}")
    private String bucketName;

    @Autowired
    public DataUploadedListener(MetaInfRepository metaInfRepository, MinioClient minioClient,
                                SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.metaInfRepository = metaInfRepository;
        this.minioClient = minioClient;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "data_uploaded", groupId = "data-uploaded")
    @Transactional
    public void handleDataUploaded(String message) {
        try {
            // Десериализация сообщения
            DataUploaded dataUploaded = objectMapper.readValue(message, DataUploaded.class);
            String batchId = dataUploaded.getBatchId();
            List<String> fileNames = dataUploaded.getFileNames();

            log.info("Received message for batchId: {}, fileNames: {}", batchId, fileNames);

            // Получение файлов из MinIO
            for (String fileName : fileNames) {
                try (InputStream inputStream = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(bucketName)
                                .object(fileName)
                                .build())) {
                    // Здесь можно обработать файл, если нужно (например, сохранить локально или передать в модель)
                    log.info("Successfully retrieved file {} from MinIO", fileName);
                } catch (Exception e) {
                    log.error("Error retrieving file {} from MinIO: {}", fileName, e.getMessage());
                    throw new RuntimeException("Failed to retrieve file: " + fileName, e);
                }
            }

            // Создание и сохранение объекта MetaInf
            MetaInf metaInf = new MetaInf();
            metaInf.setBatchId(batchId);
            metaInf.setFileNames(fileNames.toArray(new String[0]));
            metaInf.setTargetFileName("target_" + batchId + ".csv"); // Пример имени целевого файла
            metaInf.setUploadTime(LocalDateTime.now()); // Устанавливаем время вручную, если не используется DEFAULT
            metaInfRepository.save(metaInf);

            log.info("Saved MetaInf for batchId: {}", batchId);

            // Вызов API модели (заглушка)
            callModelApi(fileNames, batchId);

            // Отправка сообщения через WebSocket
            messagingTemplate.convertAndSend("/topic/process-status",
                    "Processing started for batchId: " + batchId);

        } catch (Exception e) {
            log.error("Error processing message: {}", e.getMessage());
            throw new RuntimeException("Error processing data_uploaded message", e);
        }
    }

    // Заглушка для вызова API модели
    private void callModelApi(List<String> fileNames, String batchId) {
        // TODO: Реализовать вызов API модели
        log.info("Calling model API for batchId: {}, files: {}", batchId, fileNames);
        // Пример: Отправка HTTP-запроса к модели
        // RestTemplate restTemplate = new RestTemplate();
        // restTemplate.postForEntity("http://model-api/process", fileNames, String.class);
    }
}