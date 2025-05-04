package com.work.matmode.upload_service.web;


import com.work.matmode.upload_service.service.kafka.KafkaProducerService;
import com.work.matmode.upload_service.service.minio.MinioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/data")
@RequiredArgsConstructor
@CrossOrigin
public class DataController {

    private final MinioService minioService;

    private final KafkaProducerService kafkaProducerService;


    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadDocuments(@RequestParam("files") List<MultipartFile> files, @RequestParam("batchId") String batchId) {
        try {
            List<String> uploadedFileNames = minioService.uploadMultipleFiles(files, batchId);
            kafkaProducerService.sendDataUploadedMessage(uploadedFileNames, batchId);
            Map<String, Object> response = new HashMap<>();
            response.put("batchId", batchId);
            response.put("files", uploadedFileNames);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error uploading files: " + e.getMessage()));
        }
    }

    @PostMapping("/target/upload")
    public ResponseEntity<List<String>> uploadTarget(@RequestParam("file") MultipartFile file,
                                                     @RequestParam("batchId") String batchId) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(List.of("No file provided"));
            }
            String uploadedFileName = minioService.uploadTargetFile(file, batchId);
            kafkaProducerService.sendTargetUploadedMessage(uploadedFileName, batchId);
            return ResponseEntity.ok(List.of(uploadedFileName));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(List.of("Error uploading target file: " + e.getMessage()));
        }
    }

}
