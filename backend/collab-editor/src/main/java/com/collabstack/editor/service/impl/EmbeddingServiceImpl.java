package com.collabstack.editor.service.impl;

import com.collabstack.editor.entity.DocumentChunk;
import com.collabstack.editor.repository.DocumentChunkRepository;
import com.collabstack.editor.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingServiceImpl implements EmbeddingService {

    private final DocumentChunkRepository chunkRepository;

    // Optional — only available when app.rag.enabled=true
    @Autowired(required = false)
    private VectorStore vectorStore;

    @Override
    @Async
    @Transactional
    public void indexDocument(UUID documentId, String fullContent) {
        doIndexDocument(documentId, fullContent);
    }

    @Override
    @Transactional
    public void indexDocumentSync(UUID documentId, String fullContent) {
        doIndexDocument(documentId, fullContent);
    }

    private void doIndexDocument(UUID documentId, String fullContent) {
        if (fullContent == null || fullContent.isBlank()) {
            log.debug("Skipping indexing for document {} — empty content", documentId);
            return;
        }

        // Delete old chunks from JPA table
        chunkRepository.deleteAllByDocumentId(documentId);

        // Split into chunks
        List<String> chunks = chunkText(fullContent, 800, 100);
        log.info("Indexing document {} — {} chunks", documentId, chunks.size());

        List<Document> aiDocuments = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            String chunkContent = chunks.get(i);

            // Save chunk to JPA document_chunks table
            DocumentChunk saved = chunkRepository.save(DocumentChunk.builder()
                    .documentId(documentId)
                    .chunkIndex(i)
                    .content(chunkContent)
                    .build());

            // Build Spring AI document — use our JPA UUID as the vector store doc ID
            if (vectorStore != null) {
                Document aiDoc = new Document(
                        saved.getId().toString(),
                        chunkContent,
                        Map.of("documentId", documentId.toString(), "chunkIndex", i)
                );
                aiDocuments.add(aiDoc);
            }
        }

        // Add all to vector store (embeddings generated here)
        if (vectorStore != null && !aiDocuments.isEmpty()) {
            try {
                vectorStore.add(aiDocuments);
                log.info("Indexed {} chunks into vector store for document {}", aiDocuments.size(), documentId);
            } catch (Exception e) {
                log.error("Failed to add chunks to vector store for document {}: {}", documentId, e.getMessage());
            }
        } else if (vectorStore == null) {
            log.warn("VectorStore not available (app.rag.enabled=false) — chunks saved to DB only");
        }
    }

    @Override
    @Async
    @Transactional
    public void reindexChunk(UUID documentId, int chunkIndex, String chunkContent) {
        // For MVP: just re-run full document indexing is simpler
        // A targeted single-chunk reindex would need to find and delete the specific vector by id
        log.debug("reindexChunk called for doc {} chunk {} — delegating to full reindex is preferred",
                documentId, chunkIndex);
    }

    @Override
    @Async
    @Transactional
    public void deleteDocumentChunks(UUID documentId) {
        // Get chunk IDs before deleting from JPA
        List<DocumentChunk> chunks = chunkRepository.findAllByDocumentIdOrderByChunkIndexAsc(documentId);
        List<String> vectorIds = chunks.stream().map(c -> c.getId().toString()).toList();

        chunkRepository.deleteAllByDocumentId(documentId);

        if (vectorStore != null && !vectorIds.isEmpty()) {
            try {
                vectorStore.delete(vectorIds);
                log.info("Deleted {} vector chunks for document {}", vectorIds.size(), documentId);
            } catch (Exception e) {
                log.error("Failed to delete vector chunks for document {}: {}", documentId, e.getMessage());
            }
        }
    }

    /**
     * Splits text into chunks of `size` characters with `overlap` character overlap.
     */
    List<String> chunkText(String text, int size, int overlap) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + size, text.length());
            chunks.add(text.substring(start, end));
            if (end == text.length()) break;
            start += (size - overlap);
        }
        return chunks;
    }
}
