
package com.example.documentstorage.storage;

import java.time.Instant;
import java.util.List;
import java.io.InputStream;

public interface DocumentStorage {

    StoragePage listObjects(
            String prefix,
            String startAfter,
            int maxKeys
    );

    boolean objectExists(String key);

    record StoredDocument(
            String key,
            long sizeBytes,
            Instant lastModified
    ) {}

    record StoragePage(
            List<StoredDocument> documents,
            boolean truncated,
            String nextStartAfter
    ) {}

    void uploadObject(String key, InputStream inputStream, long contentLength,
                  String contentType);
}
