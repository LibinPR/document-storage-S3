
package com.example.documentstorage.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import static org.mockito.ArgumentMatchers.eq;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.documentstorage.storage.DocumentStorage;
import com.example.documentstorage.storage.DocumentStorage.StoredDocument;
import com.example.documentstorage.storage.DocumentStorage.StoragePage;

class DocumentSearchServiceTest {

    private DocumentStorage storage;
    private DocumentSearchService searchService;

    @BeforeEach
    void setUp() {
        storage = mock(DocumentStorage.class);

        CursorService cursorService =
                new CursorService("test-secret-that-is-at-least-32-characters");

        searchService = new DocumentSearchService(
                storage, cursorService, 20, 100);
    }

    @Test
    void searchMatchesFileNamesCaseInsensitively() {
        when(storage.listObjects("users/libin/", null, 100))
                .thenReturn(new StoragePage(List.of(
                        document("users/libin/Invoice.PDF"),
                        document("users/libin/profile.jpg"),
                        document("users/libin/invoice-2026.pdf")
                ), false, null));

        DocumentSearchResponse response =
                searchService.search("libin", "INVOICE", null, null);

        assertEquals(2, response.documents().size());
        assertEquals(
                List.of("Invoice.PDF", "invoice-2026.pdf"),
                response.documents().stream()
                        .map(DocumentSearchResponse.DocumentItem::fileName)
                        .toList());
        assertEquals(null, response.nextCursor());
    }

    @Test
    void rejectsPageSizeOutsideAllowedRange() {
        assertThrows(IllegalArgumentException.class,
                () -> searchService.search("libin", null, 0, null));

        assertThrows(IllegalArgumentException.class,
                () -> searchService.search("libin", null, 101, null));

        verify(storage, never())
                .listObjects(anyString(), isNull(), anyInt());
    }

    @Test
    void rejectsTamperedCursor() {
        when(storage.listObjects("users/libin/", null, 100))
                .thenReturn(new StoragePage(List.of(
                        document("users/libin/a.pdf"),
                        document("users/libin/b.pdf")
                ), false, null));

        DocumentSearchResponse firstPage =
                searchService.search("libin", null, 1, null);

        assertNotNull(firstPage.nextCursor());

        String tamperedCursor = firstPage.nextCursor() + "x";

        assertThrows(IllegalArgumentException.class,
                () -> searchService.search(
                        "libin", null, 1, tamperedCursor));
    }

    
    @Test
    void doesNotSkipLookaheadDocumentOnNextPage() {
        when(storage.listObjects("users/libin/", null, 100))
                .thenReturn(new StoragePage(List.of(
                        document("users/libin/a.pdf"),
                        document("users/libin/b.pdf"),
                        document("users/libin/c.pdf")
                ), false, null));

        when(storage.listObjects(
                org.mockito.ArgumentMatchers.eq("users/libin/"),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(100)))
                .thenReturn(new StoragePage(List.of(
                        document("users/libin/c.pdf")
                ), false, null));

        DocumentSearchResponse firstPage =
                searchService.search("libin", null, 2, null);

        assertEquals(
                List.of("a.pdf", "b.pdf"),
                firstPage.documents().stream()
                        .map(DocumentSearchResponse.DocumentItem::fileName)
                        .toList());
        assertNotNull(firstPage.nextCursor());

        CursorService cursorService =
            new CursorService("test-secret-that-is-at-least-32-characters");

        String decodedCursor = cursorService.decode(firstPage.nextCursor());

        org.junit.jupiter.api.Assertions.assertTrue(
                decodedCursor.endsWith("\nusers/libin/b.pdf"),
                "Cursor should point to the last returned document, not the lookahead document");

        DocumentSearchResponse secondPage =
                searchService.search(
                        "libin", null, 2, firstPage.nextCursor());

        assertEquals(
                List.of("c.pdf"),
                secondPage.documents().stream()
                        .map(DocumentSearchResponse.DocumentItem::fileName)
                        .toList());
    }


    private StoredDocument document(String key) {
        return new StoredDocument(
                key, 1024L, Instant.parse("2026-10-22T10:00:00Z"));
    }
}
