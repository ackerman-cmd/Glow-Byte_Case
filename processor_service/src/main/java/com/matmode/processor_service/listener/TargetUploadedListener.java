package com.matmode.processor_service.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matmode.processor_service.model.Fire;
import com.matmode.processor_service.repository.FireRepository;
import com.matmode.processor_service.service.dto.TargetUploaded;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Slf4j
@Component
public class TargetUploadedListener {

    private final FireRepository fireRepository;
    private final MinioClient minioClient;
    private final ObjectMapper objectMapper;

    @Value("${minio.bucket}")
    private String bucketName;

    @Autowired
    public TargetUploadedListener(FireRepository fireRepository, MinioClient minioClient, ObjectMapper objectMapper) {
        this.fireRepository = fireRepository;
        this.minioClient = minioClient;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "target_uploaded", groupId = "target-uploaded")
    @Transactional
    public void handleTargetUploaded(String message) {
        try {
            // Deserialize Kafka message
            TargetUploaded targetUploaded = objectMapper.readValue(message, TargetUploaded.class);
            String batchId = targetUploaded.getBatchId();
            String fileName = targetUploaded.getFileName();

            log.info("Processing file: {} for batchId: {}", fileName, batchId);

            // Retrieve file from MinIO
            try (var inputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build());
                 var reader = new BufferedReader(new InputStreamReader(inputStream))) {

                // Parse CSV file, skipping header
                String line;
                boolean isFirstLine = true;
                while ((line = reader.readLine()) != null) {
                    if (isFirstLine) {
                        isFirstLine = false;
                        continue;
                    }
                    Fire fire = parseCsvLineToFire(line, batchId);
                    fireRepository.save(fire);
                    log.debug("Saved Fire entity for batchId: {}, line: {}", batchId, line);
                }
                log.info("Completed processing file: {} for batchId: {}", fileName, batchId);
            } catch (Exception e) {
                log.error("Failed to retrieve or process file: {} for batchId: {}", fileName, batchId, e);
                throw new RuntimeException("Failed to process file: " + fileName, e);
            }

        } catch (Exception e) {
            log.error("Failed to process Kafka message: {}", message, e);
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
                log.error("Number parsing error in CSV line: {} for batchId: {}, column: {}", line, batchId, getFailedColumn(columns,e), e);
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
}