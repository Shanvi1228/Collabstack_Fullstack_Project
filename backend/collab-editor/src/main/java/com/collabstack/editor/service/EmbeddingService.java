package com.collabstack.editor.service;

import java.util.UUID;

public interface EmbeddingService {
    void indexDocument(UUID documentId, String fullContent);
    void indexDocumentSync(UUID documentId, String fullContent);
    void reindexChunk(UUID documentId, int chunkIndex, String chunkContent);
    void deleteDocumentChunks(UUID documentId);
}
