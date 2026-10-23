package com.example.documentstorage.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.example.documentstorage.service.DocumentNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(
            IllegalArgumentException exception) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "Bad Request",
                        "message", exception.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleConfigurationError(
            IllegalStateException exception) {

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "Internal Server Error",
                        "message", "Application configuration error"));
    }

    
        @ExceptionHandler(DocumentNotFoundException.class)
        public ResponseEntity<Map<String, String>> handleDocumentNotFound(
                DocumentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
        }

        @ExceptionHandler(MaxUploadSizeExceededException.class)
        public ResponseEntity<Map<String, String>> handleMaxUploadSizeExceeded(
                MaxUploadSizeExceededException exception) {

         return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
            .body(Map.of(
                    "error", "Payload Too Large",
                    "message", "File size must not exceed 10 MB"));
        }

}
