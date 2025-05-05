package com.matmode.processor_service.service;

import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@Slf4j
public class MinioService {

    private final MinioClient minioClient;

    private String bucketName;

    public MinioService(MinioClient minioClient, @Value("${minio.bucket}") String bucketName) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
    }

    public String uploadFile(MultipartFile file) throws Exception {
        String fileName = UUID.randomUUID().toString() + "-" + file.getOriginalFilename();

        minioClient.putObject(
                         PutObjectArgs
                        .builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build());

        return fileName;
    }

    public String uploadTargetFile(MultipartFile file, String batchId) throws Exception {
        String fileName = batchId + "-" + file.getOriginalFilename();
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build());
        return fileName;
    }

    public List<String> uploadMultipleFiles(List<MultipartFile> files, String batchId) throws Exception {
        List<String> uploadedFileNames = new ArrayList<>();

        try {
            for (MultipartFile file : files) {
                String fileName = batchId + "-" + file.getOriginalFilename();

                // Загружаем каждый файл в MinIO
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(fileName)
                                .stream(file.getInputStream(), file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build()
                );

                uploadedFileNames.add(fileName); // Сохраняем имена загруженных файлов
            }

        } catch (Exception e) {
            // Если ошибка, удаляем все загруженные файлы
            for (String fileName : uploadedFileNames) {
                try {
                    minioClient.removeObject(
                            RemoveObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(fileName)
                                    .build()
                    );
                } catch (Exception ignore) {
                    // Логируем ошибку удаления, но не останавливаем процесс
                    log.info("Exception {}", ignore.getMessage());
                }
            }
            throw e;  // Прокидываем исключение дальше
        }

        return uploadedFileNames;  // Возвращаем список успешно загруженных файлов
    }


    public String getFileUrl(String fileName) throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucketName)
                        .object(fileName)
                        .expiry(60 * 60 * 60) // 60 hour
                        .build());
    }

    public void updateFile(String fileName, MultipartFile file) throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );



    }

    public InputStream downloadFile(String fileName) throws Exception {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .build());
    }

    public void deleteFile(String fileName) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .build());
    }

    public List<String> listAllFiles() throws Exception {
        List<String> fileNames = new ArrayList<>();

        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .recursive(true)
                        .build()
        );

        for (Result<Item> result : results) {
            Item item = result.get();
            fileNames.add(item.objectName());
        }

        return fileNames;
    }



}
