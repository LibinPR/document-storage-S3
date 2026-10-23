
package com.example.documentstorage.service;

import java.time.Instant;
import java.util.List;

public record DocumentSearchResponse(
        List<DocumentItem> documents,
        String nextCursor
) {
    public record DocumentItem(
            String fileName,
            String key,
            long sizeBytes,
            Instant lastModified
    ) {}
}
