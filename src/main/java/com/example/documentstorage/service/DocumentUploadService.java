
package com.example.documentstorage.service;

import java.io.IOException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.documentstorage.storage.DocumentStorage;

@Service
public class DocumentUploadService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private final DocumentStorage storage;

    public DocumentUploadService(DocumentStorage storage) {
        this.storage = storage;
    }

    public String upload(String userName, MultipartFile file) {
        if (userName == null
                || !userName.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new IllegalArgumentException("Invalid userName");
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "File size must not exceed 10 MB");
        }

        String originalFileName = file.getOriginalFilename();

        if (originalFileName == null
                || originalFileName.isBlank()
                || originalFileName.contains("/")
                || originalFileName.contains("\\")
                || originalFileName.equals(".")
                || originalFileName.equals("..")) {
            throw new IllegalArgumentException("Invalid file name");
        }

        String key = "users/" + userName + "/" + originalFileName;
        String contentType = file.getContentType() == null
                ? "application/octet-stream"
                : file.getContentType();

        try (var inputStream = file.getInputStream()) {
            storage.uploadObject(
                    key, inputStream, file.getSize(), contentType);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read uploaded file");
        }

        return key;
    }
}
