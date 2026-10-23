
package com.example.documentstorage.service;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.documentstorage.storage.DocumentStorage;

@Service
public class DocumentUploadService {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final int MAX_FILENAME_LENGTH = 255;

    private static final Map<String, Set<String>> ALLOWED_TYPES = Map.of(
            ".pdf", Set.of("application/pdf"),
            ".txt", Set.of("text/plain"),
            ".md", Set.of("text/markdown", "text/plain"),
            ".docx", Set.of(
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            ".csv", Set.of("text/csv", "application/csv", "text/plain")
    );

    private final DocumentStorage storage;

    public DocumentUploadService(DocumentStorage storage) {
        this.storage = storage;
    }

    public String upload(String userName, MultipartFile file) {
        validateUserName(userName);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "File size must not exceed 10 MB");
        }

        String originalFileName = file.getOriginalFilename();
        String extension = validateFileName(originalFileName);
        validateContentType(extension, file.getContentType());

        String key = "users/" + userName + "/" + originalFileName;
        String contentType = file.getContentType() == null
                || file.getContentType().isBlank()
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

    private void validateUserName(String userName) {
        if (userName == null
                || !userName.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new IllegalArgumentException("Invalid userName");
        }
    }

    private String validateFileName(String fileName) {
        if (fileName == null
                || fileName.isBlank()
                || fileName.length() > MAX_FILENAME_LENGTH
                || fileName.equals(".")
                || fileName.equals("..")
                || fileName.contains("/")
                || fileName.contains("\\")
                || fileName.chars().anyMatch(Character::isISOControl)
                || fileName.endsWith(".")
                || fileName.endsWith(" ")) {
            throw new IllegalArgumentException("Invalid file name");
        }

        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex <= 0 || dotIndex == fileName.length() - 1) {
            throw new IllegalArgumentException(
                    "Unsupported file type. Allowed: PDF, TXT, MD, DOCX, CSV");
        }

        String extension = fileName.substring(dotIndex).toLowerCase(Locale.ROOT);

        if (!ALLOWED_TYPES.containsKey(extension)) {
            throw new IllegalArgumentException(
                    "Unsupported file type. Allowed: PDF, TXT, MD, DOCX, CSV");
        }

        return extension;
    }

    private void validateContentType(String extension, String contentType) {
        // Some clients omit Content-Type. The extension allowlist still applies.
        if (contentType == null || contentType.isBlank()) {
            return;
        }

        String normalizedType = contentType
                .split(";", 2)[0]
                .trim()
                .toLowerCase(Locale.ROOT);

        if (!ALLOWED_TYPES.get(extension).contains(normalizedType)) {
            throw new IllegalArgumentException(
                    "Content-Type does not match the file extension");
        }
    }
}
