package com.example.documentstorage.storage;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.InputStream;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
public class S3DocumentStorage implements DocumentStorage {

    private final S3Client s3Client;
    private final String bucketName;

    public S3DocumentStorage(
            S3Client s3Client,
            @Value("${app.aws.s3-bucket}") String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    @Override
    public StoragePage listObjects(
            String prefix,
            String startAfter,
            int maxKeys) {

        ListObjectsV2Request.Builder request =
                ListObjectsV2Request.builder()
                        .bucket(bucketName)
                        .prefix(prefix)
                        .maxKeys(maxKeys);

        if (startAfter != null && !startAfter.isBlank()) {
            request.startAfter(startAfter);
        }

        ListObjectsV2Response response =
                s3Client.listObjectsV2(request.build());

        List<StoredDocument> documents =
                response.contents().stream()
                        .map(this::toStoredDocument)
                        .toList();

        String nextStartAfter = null;

        if (Boolean.TRUE.equals(response.isTruncated())
                && !response.contents().isEmpty()) {
            nextStartAfter = response.contents()
                    .get(response.contents().size() - 1)
                    .key();
        }

        return new StoragePage(
                documents,
                Boolean.TRUE.equals(response.isTruncated()),
                nextStartAfter
        );
    }

    @Override
    public boolean objectExists(String key) {
        try {
        s3Client.headObject(request -> request
                        .bucket(bucketName)
                        .key(key));
                return true;
        } catch (software.amazon.awssdk.services.s3.model.NoSuchKeyException e) {
                return false;
        } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
                if (e.statusCode() == 404) {
                return false;
                }
                throw e;
        }
    }

    @Override
    public void uploadObject(
                String key,
                InputStream inputStream,
                long contentLength,
                String contentType) {

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        s3Client.putObject(
                request,
                RequestBody.fromInputStream(inputStream, contentLength));
        }

    private StoredDocument toStoredDocument(S3Object object) {
        return new StoredDocument(
                object.key(),
                object.size(),
                object.lastModified()
        );
    }
}
