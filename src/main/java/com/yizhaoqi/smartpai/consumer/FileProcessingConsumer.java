package com.yizhaoqi.smartpai.consumer;

import com.yizhaoqi.smartpai.config.KafkaConfig;
import com.yizhaoqi.smartpai.model.FileProcessingTask;
import com.yizhaoqi.smartpai.service.ParseService;
import com.yizhaoqi.smartpai.service.VectorizationService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@Service
@Slf4j
public class FileProcessingConsumer {

    private final ParseService parseService;
    private final VectorizationService vectorizationService;
    private final MinioClient minioClient;

    @Autowired
    private KafkaConfig kafkaConfig;

    @Value("${minio.bucketName:uploads}")
    private String bucketName;

    public FileProcessingConsumer(ParseService parseService,
                                  VectorizationService vectorizationService,
                                  MinioClient minioClient) {
        this.parseService = parseService;
        this.vectorizationService = vectorizationService;
        this.minioClient = minioClient;
    }

    @KafkaListener(topics = "#{kafkaConfig.getFileProcessingTopic()}", groupId = "#{kafkaConfig.getFileProcessingGroupId()}")
    public void processTask(FileProcessingTask task) {
        log.info("Received task: {}", task);
        log.info("文件权限信息: userId={}, orgTag={}, isPublic={}",
                task.getUserId(), task.getOrgTag(), task.isPublic());

        InputStream fileStream = null;
        try {
            fileStream = downloadFileFromStorage(task);
            if (fileStream == null) {
                throw new IOException("流为空");
            }

            if (!fileStream.markSupported()) {
                fileStream = new BufferedInputStream(fileStream);
            }

            parseService.parseAndSave(task.getFileMd5(), fileStream,
                    task.getUserId(), task.getOrgTag(), task.isPublic());
            log.info("文件解析完成，fileMd5: {}", task.getFileMd5());

            vectorizationService.vectorize(task.getFileMd5(),
                    task.getUserId(), task.getOrgTag(), task.isPublic());
            log.info("向量化完成，fileMd5: {}", task.getFileMd5());
        } catch (Exception e) {
            log.error("Error processing task: {}", task, e);
            throw new RuntimeException("Error processing task", e);
        } finally {
            if (fileStream != null) {
                try {
                    fileStream.close();
                } catch (IOException e) {
                    log.error("Error closing file stream", e);
                }
            }
        }
    }

    private InputStream downloadFileFromStorage(FileProcessingTask task) throws Exception {
        String filePath = task.getFilePath();
        log.info("Downloading file from storage: {}", filePath);

        try {
            return downloadFromMinio(task);
        } catch (Exception e) {
            log.warn("Direct MinIO download failed for fileMd5={}, fallback to filePath. cause={}",
                    task.getFileMd5(), e.getMessage());
        }

        if (filePath == null || filePath.isBlank()) {
            throw new IOException("File path is empty");
        }

        File file = new File(filePath);
        if (file.exists()) {
            log.info("Detected file system path: {}", filePath);
            return new FileInputStream(file);
        }

        if (filePath.startsWith("http://") || filePath.startsWith("https://")) {
            log.info("Detected remote URL: {}", filePath);
            URL url = new URL(filePath);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(180000);
            connection.setRequestProperty("User-Agent", "SmartPAI-FileProcessor/1.0");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                log.info("Successfully connected to URL, starting download...");
                return connection.getInputStream();
            }
            if (responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                throw new IOException("Access forbidden - the presigned URL may have expired");
            }
            throw new IOException("Failed to download file, HTTP response code: " + responseCode);
        }

        throw new IllegalArgumentException("Unsupported file path format: " + filePath);
    }

    private InputStream downloadFromMinio(FileProcessingTask task) throws Exception {
        String mergedByMd5 = "merged/" + task.getFileMd5();
        try {
            log.info("Trying direct MinIO object by md5: {}", mergedByMd5);
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(mergedByMd5)
                            .build()
            );
        } catch (Exception e) {
            log.warn("Direct MinIO md5 object not available: {}, cause={}", mergedByMd5, e.getMessage());
        }

        String mergedByName = "merged/" + task.getFileName();
        log.info("Trying fallback MinIO object by file name: {}", mergedByName);
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(mergedByName)
                        .build()
        );
    }
}
