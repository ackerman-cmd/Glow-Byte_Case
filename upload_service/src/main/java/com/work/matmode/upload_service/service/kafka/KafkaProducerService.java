package com.work.matmode.upload_service.service.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.work.matmode.upload_service.service.kafka.dto.DataUploaded;
import com.work.matmode.upload_service.service.kafka.dto.TargetUploaded;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducerService.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.kafka.topics:data_uploaded,target_uploaded}")
    private String[] topics;

    @Async("taskExecutor")
    public void sendDataUploadedMessage(List<String> fileNames, String batchId) {
        try {
            DataUploaded dataUploaded = DataUploaded
                    .builder()
                    .fileNames(fileNames)
                    .batchId(batchId)
                    .build();

            String messageJson = objectMapper.writeValueAsString(dataUploaded);
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topics[0], messageJson);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Sent message=[{}] with offset=[{}] to topic=[{}]",
                            messageJson, result.getRecordMetadata().offset(), topics[0]);
                } else {
                    log.error("Unable to send message=[{}] to topic=[{}] due to: {}",
                            messageJson, topics[0], ex.getMessage());
                }
            });
        } catch (JsonProcessingException e) {
            log.error("Error serializing message for topic [{}]: {}", topics[0], e.getMessage());
            throw new RuntimeException("Error serializing message", e);
        }
    }

    @Async("taskExecutor")
    public void sendTargetUploadedMessage(String fileName, String batchId) {
        try {
            TargetUploaded targetUploaded =
                    TargetUploaded
                            .builder()
                            .fileName(fileName)
                            .batchId(batchId)
                            .build();
            String messageJson = objectMapper.writeValueAsString(targetUploaded);
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topics[1], messageJson);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Sent message=[{}] with offset=[{}] to topic=[{}]",
                            messageJson, result.getRecordMetadata().offset(), topics[1]);
                } else {
                    log.error("Unable to send message=[{}] to topic=[{}] due to: {}",
                            messageJson, topics[1], ex.getMessage());
                }
            });
        } catch (JsonProcessingException e) {
            log.error("Error serializing message for topic [{}]: {}", topics[1], e.getMessage());
            throw new RuntimeException("Error serializing message", e);
        }
    }
}