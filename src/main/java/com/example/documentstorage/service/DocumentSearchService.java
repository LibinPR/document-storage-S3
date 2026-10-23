
package com.example.documentstorage.service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.documentstorage.storage.DocumentStorage;
import com.example.documentstorage.storage.DocumentStorage.StoredDocument;
import com.example.documentstorage.storage.DocumentStorage.StoragePage;

@Service
public class DocumentSearchService {

    private final DocumentStorage storage;
    private final CursorService cursorService;
    private final int defaultPageSize;
    private final int maxPageSize;

    public DocumentSearchService(
            DocumentStorage storage,
            CursorService cursorService,
            @Value("${app.documents.default-page-size:20}") int defaultPageSize,
            @Value("${app.documents.max-page-size:100}") int maxPageSize) {
        this.storage = storage;
        this.cursorService = cursorService;
        this.defaultPageSize = defaultPageSize;
        this.maxPageSize = maxPageSize;
    }

    
    public DocumentSearchResponse search(
            String userName,
            String query,
            Integer requestedPageSize,
            String cursor) {

        validateUserName(userName);

        int pageSize = requestedPageSize == null
                ? defaultPageSize
                : requestedPageSize;

        if (pageSize < 1 || pageSize > maxPageSize) {
            throw new IllegalArgumentException(
                    "pageSize must be between 1 and " + maxPageSize);
        }

        String normalizedQuery = query == null
                ? ""
                : query.trim().toLowerCase(Locale.ROOT);

        String prefix = "users/" + userName + "/";
        String startAfter = decodeCursor(
                cursor, userName, normalizedQuery, pageSize);

        List<DocumentSearchResponse.DocumentItem> matches =
                new ArrayList<>();

        String lastScannedKey = startAfter;
        String lastReturnedKey = null;
        boolean hasMore = false;

        while (matches.size() < pageSize + 1) {
            StoragePage page = storage.listObjects(
                    prefix, lastScannedKey, Math.max(100, pageSize));

            if (page.documents().isEmpty()) {
                break;
            }

            for (StoredDocument document : page.documents()) {
                lastScannedKey = document.key();

                String fileName = fileName(document.key());

                if (fileName.toLowerCase(Locale.ROOT)
                        .contains(normalizedQuery)) {

                    if (matches.size() == pageSize) {
                        hasMore = true;
                        break;
                    }

                    matches.add(
                            new DocumentSearchResponse.DocumentItem(
                                    fileName,
                                    document.key(),
                                    document.sizeBytes(),
                                    document.lastModified()));

                    lastReturnedKey = document.key();
                }
            }

            if (hasMore || !page.truncated()) {
                break;
            }

            if (page.nextStartAfter() == null
                    || page.nextStartAfter().equals(lastScannedKey)) {
                break;
            }
        }

        String nextCursor = hasMore
                ? encodeCursor(
                        userName,
                        normalizedQuery,
                        pageSize,
                        lastReturnedKey)
                : null;

        return new DocumentSearchResponse(matches, nextCursor);
    }


    private String encodeCursor(
            String userName, String query, int pageSize, String lastKey) {
        String raw = String.join("\n",
                userName, query, Integer.toString(pageSize), lastKey);

        return cursorService.encode(raw);
    }

    private String decodeCursor(
            String cursor, String userName, String query, int pageSize) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }

        String decoded = cursorService.decode(cursor);
        String[] parts = decoded.split("\n", 4);

        if (parts.length != 4
                || !parts[0].equals(userName)
                || !parts[1].equals(query)
                || !parts[2].equals(Integer.toString(pageSize))) {
            throw new IllegalArgumentException(
                    "Cursor does not match the current search");
        }

        return parts[3];
    }

    private String fileName(String key) {
        int separator = key.lastIndexOf('/');
        return separator >= 0 ? key.substring(separator + 1) : key;
    }

    private void validateUserName(String userName) {
        if (userName == null
                || !userName.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new IllegalArgumentException("Invalid userName");
        }
    }
}
