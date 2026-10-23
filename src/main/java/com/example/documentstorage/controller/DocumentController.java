
package com.example.documentstorage.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.example.documentstorage.service.DocumentDownloadService;

import com.example.documentstorage.service.DocumentSearchResponse;
import com.example.documentstorage.service.DocumentSearchService;
import com.example.documentstorage.service.DocumentUploadService;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users/{userName}/documents")
public class DocumentController {

    private final DocumentSearchService searchService;
    private final DocumentDownloadService downloadService;
    private final DocumentUploadService uploadService;

    public DocumentController(
        DocumentSearchService searchService,
        DocumentDownloadService downloadService,
        DocumentUploadService uploadService) {
        this.searchService = searchService;
        this.downloadService = downloadService;
        this.uploadService = uploadService;
    }

    @GetMapping
    public DocumentSearchResponse searchDocuments(
            @PathVariable String userName,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String cursor) {

        return searchService.search(userName, query, pageSize, cursor);
    }

    @GetMapping("/{fileName}/download")
    public Map<String, String> downloadDocument(
        @PathVariable("userName") String userName,
        @PathVariable("fileName") String fileName) {
            String url = downloadService.createDownloadUrl(userName, fileName);
        return Map.of("downloadUrl" , url);
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<Map<String , String>> uploadDocument(
        @PathVariable("userName") String userName,
        @RequestPart("file") MultipartFile file
    ) {
        String key = uploadService.upload(userName , file);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of(
                "message" , "Document uploaded successfully",
                "fileName" , file.getOriginalFilename(),
                "key" , key));
    }
    
}
