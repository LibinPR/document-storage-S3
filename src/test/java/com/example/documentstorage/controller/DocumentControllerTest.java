
package com.example.documentstorage.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import com.example.documentstorage.service.DocumentDownloadService;
import com.example.documentstorage.service.DocumentSearchResponse;
import com.example.documentstorage.service.DocumentSearchService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import com.example.documentstorage.service.DocumentUploadService;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import org.springframework.mock.web.MockMultipartFile;

@WebMvcTest(DocumentController.class)
@Import(com.example.documentstorage.exception.GlobalExceptionHandler.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentSearchService searchService;

    @MockBean
    private DocumentDownloadService downloadService;

    @MockBean
    private DocumentUploadService uploadService;

    @Test
    void searchReturnsDocuments() throws Exception {
        var item = new DocumentSearchResponse.DocumentItem(
                "logistics.pdf",
                "users/libin/logistics.pdf",
                120L,
                Instant.parse("2026-10-23T10:00:00Z"));

        when(searchService.search("libin", "logistics", 20, null))
                .thenReturn(new DocumentSearchResponse(List.of(item), null));

        mockMvc.perform(get("/api/v1/users/libin/documents")
                        .param("query", "logistics")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documents[0].fileName")
                        .value("logistics.pdf"))
                .andExpect(jsonPath("$.documents[0].key")
                        .value("users/libin/logistics.pdf"))
                .andExpect(jsonPath("$.documents[0].sizeBytes").value(120))
                .andExpect(jsonPath("$.nextCursor").doesNotExist());

        verify(searchService).search("libin", "logistics", 20, null);
    }

    @Test
    void searchReturnsBadRequestForInvalidPageSize() throws Exception {
        when(searchService.search("libin", "logistics", 0, null))
                .thenThrow(new IllegalArgumentException(
                        "pageSize must be between 1 and 100"));

        mockMvc.perform(get("/api/v1/users/libin/documents")
                        .param("query", "logistics")
                        .param("pageSize", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message")
                        .value("pageSize must be between 1 and 100"));
    }

    @Test
    void downloadReturnsPresignedUrl() throws Exception {
        when(downloadService.createDownloadUrl(
                "libin", "logistics.pdf"))
                .thenReturn("https://example.com/presigned-download");

        mockMvc.perform(get(
                        "/api/v1/users/libin/documents/logistics.pdf/download"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.downloadUrl")
                        .value("https://example.com/presigned-download"));
    }

    @Test
    void downloadReturnsNotFoundForMissingDocument() throws Exception {
        when(downloadService.createDownloadUrl(
                "libin", "missing.pdf"))
                .thenThrow(new com.example.documentstorage.service.DocumentNotFoundException(
                        "Document not found"));

        mockMvc.perform(get(
                        "/api/v1/users/libin/documents/missing.pdf/download"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Document not found"));
    }

    
    @Test
    void searchUsesRequestedUserNameAndQuery() throws Exception {
        when(searchService.search("libin", "logistics", 20, null))
                .thenReturn(new DocumentSearchResponse(List.of(), null));

        mockMvc.perform(get("/api/v1/users/libin/documents")
                        .param("query", "logistics")
                        .param("pageSize", "20"))
                .andExpect(status().isOk());

        verify(searchService).search("libin", "logistics", 20, null);
    }

    @Test
    void uploadReturnsCreatedWhenSuccessful() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-doc.txt",
                "text/plain",
                "FreightFox upload test".getBytes());

        when(uploadService.upload("libin", file))
                .thenReturn("users/libin/test-doc.txt");

        mockMvc.perform(multipart("/api/v1/users/{userName}/documents", "libin")
                        .file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message")
                        .value("Document uploaded successfully"))
                .andExpect(jsonPath("$.fileName").value("test-doc.txt"))
                .andExpect(jsonPath("$.key")
                        .value("users/libin/test-doc.txt"));

        verify(uploadService).upload("libin", file);
    }

}
