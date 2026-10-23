package com.example.documentstorage.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import java.nio.charset.StandardCharsets;

import com.example.documentstorage.storage.DocumentStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class DocumentUploadServiceTest {

    private DocumentStorage storage;
    private DocumentUploadService service;

    @BeforeEach
    void setUp() {
        storage = mock(DocumentStorage.class);
        service = new DocumentUploadService(storage);
    }

    private MockMultipartFile file(String name, String type, String content) {
        return new MockMultipartFile(
                "file",
                name,
                type,
                content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void uploadsAllowedTextFile() {
        var file = file("notes.txt", "text/plain", "sample content");

        String key = service.upload("libin", file);

        assertEquals("users/libin/notes.txt", key);
    }

    @Test
    void acceptsCsvFile() {
        var file = file("report.csv", "text/csv", "id,name\n1,A");

        assertEquals("users/libin/report.csv",
                service.upload("libin", file));
    }

    @Test
    void rejectsUnsupportedExtensionBeforeStorage() {
        var file = file("payload.exe", "application/octet-stream", "test");

        assertThrows(IllegalArgumentException.class,
                () -> service.upload("libin", file));

        verifyNoInteractions(storage);
    }

    @Test
    void rejectsMimeTypeThatDoesNotMatchExtension() {
        var file = file("notes.txt", "application/pdf", "sample");

        assertThrows(IllegalArgumentException.class,
                () -> service.upload("libin", file));

        verifyNoInteractions(storage);
    }

    @Test
    void rejectsPathTraversalFilename() {
        var file = file("../notes.txt", "text/plain", "sample");

        assertThrows(IllegalArgumentException.class,
                () -> service.upload("libin", file));

        verifyNoInteractions(storage);
    }

    @Test
    void rejectsFilenameLongerThan255Characters() {
        String name = "a".repeat(252) + ".txt"; // 256 characters
        var file = file(name, "text/plain", "sample");

        assertThrows(IllegalArgumentException.class,
                () -> service.upload("libin", file));

        verifyNoInteractions(storage);
    }

    @Test
    void rejectsInvalidUsername() {
        var file = file("notes.txt", "text/plain", "sample");

        assertThrows(IllegalArgumentException.class,
                () -> service.upload("../libin", file));

        verifyNoInteractions(storage);
    }

    @Test
    void rejectsEmptyFile() {
        MultipartFile file = new MockMultipartFile(
                "file", "notes.txt", "text/plain", new byte[0]);

        assertThrows(IllegalArgumentException.class,
                () -> service.upload("libin", file));

        verifyNoInteractions(storage);
    }

    @Test
    void rejectsControlCharacterInFilename() {
        var file = file("notes\n.txt", "text/plain", "sample");

        assertThrows(IllegalArgumentException.class,
                () -> service.upload("libin", file));

        verifyNoInteractions(storage);
    }
}
