
package com.example.documentstorage.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.documentstorage.storage.DocumentStorage;

import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Service
public class DocumentDownloadService {

    private final S3Presigner presigner;
    private final DocumentStorage storage;
    private final String bucketName;
    private final int ttlSeconds;

    public DocumentDownloadService(
            S3Presigner presigner,
            DocumentStorage storage,
            @Value("${app.aws.s3-bucket}") String bucketName,
            @Value("${app.documents.presigned-url-ttl-seconds:300}")
            int ttlSeconds) {

        this.presigner = presigner;
        this.storage = storage;
        this.bucketName = bucketName;
        this.ttlSeconds = ttlSeconds;
    }

    public String createDownloadUrl(String userName, String fileName) {
        if (userName == null
                || !userName.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new IllegalArgumentException("Invalid userName");
        }

        if (fileName == null
                || fileName.isBlank()
                || fileName.contains("/")
                || fileName.contains("\\")
                || fileName.equals(".")
                || fileName.equals("..")) {
            throw new IllegalArgumentException("Invalid fileName");
        }

        String key = "users/" + userName + "/" + fileName;

        if (!storage.objectExists(key)) {
            throw new DocumentNotFoundException("Document not found");
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest =
                GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofSeconds(ttlSeconds))
                        .getObjectRequest(getObjectRequest)
                        .build();

        PresignedGetObjectRequest presignedRequest =
                presigner.presignGetObject(presignRequest);

        return presignedRequest.url().toString();
    }
}
