package com.collabstack.editor.service.impl;

import com.collabstack.editor.dto.request.ChatRequest;
import com.collabstack.editor.dto.response.ChatResponse;
import com.collabstack.editor.dto.response.DocumentResponse;
import com.collabstack.editor.service.DocumentService;
import com.collabstack.editor.service.EmbeddingService;
import com.collabstack.editor.service.RagChatService;
import com.collabstack.editor.websocket.CollaborationSessionManager;
import com.collabstack.editor.websocket.DocumentSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagChatServiceImpl implements RagChatService {

    private final DocumentService documentService;
    private final ChatClient chatClient;
    private final CollaborationSessionManager sessionManager;

    // Optional — only available when app.rag.enabled=true
    @Autowired(required = false)
    private VectorStore vectorStore;

    @Autowired(required = false)
    private EmbeddingService embeddingService;

    @Override
    public ChatResponse chat(UUID documentId, UUID userId, ChatRequest request) {
        // 1. Verify user has access (throws UnauthorizedException if not)
        DocumentResponse docResponse = documentService.findById(documentId, userId);

        if (vectorStore == null) {
            return new ChatResponse(
                    "RAG is not enabled on this server. Set app.rag.enabled=true and ensure pgvector is installed.",
                    Collections.emptyList()
            );
        }

        // 2. Retrieve relevant chunks via similarity search filtered by documentId
        List<Document> relevantDocs = searchDocumentChunks(documentId, request.question());

        // 3. If no indexed content found, auto-index the document and retry
        if (relevantDocs.isEmpty() && embeddingService != null) {
            // Prefer live content from active WS session, fall back to DB snapshot
            String content = null;
            DocumentSession liveSession = sessionManager.get(documentId);
            if (liveSession != null) {
                content = liveSession.getCurrentContent();
            }
            if (content == null || content.isBlank()) {
                content = docResponse.contentSnapshot();
            }
            if (content != null && !content.isBlank()) {
                log.info("No indexed content for document {}. Auto-indexing now...", documentId);
                try {
                    embeddingService.indexDocumentSync(documentId, content);
                    // Retry the search after indexing
                    relevantDocs = searchDocumentChunks(documentId, request.question());
                } catch (Exception e) {
                    log.error("Auto-indexing failed for document {}: {}", documentId, e.getMessage());
                }
            }
        }

        if (relevantDocs.isEmpty()) {
            return new ChatResponse(
                    "This document has no content to index yet. Add some content and try again.",
                    Collections.emptyList()
            );
        }

        // 3. Build context string
        String context = relevantDocs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n---\n\n"));

        // 4. System prompt
        String systemPrompt = """
                You are a helpful assistant answering questions about a document.
                Answer ONLY using the provided document context below.
                If the answer is not in the context, say "I couldn't find that in the document."
                Be concise and cite specific parts of the document when relevant.

                DOCUMENT CONTEXT:
                """ + context;

        // 5. Call LLM
        String answer;
        try {
            answer = chatClient.prompt()
                    .system(systemPrompt)
                    .user(request.question())
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("LLM call failed for document {}: {}", documentId, e.getMessage());
            return new ChatResponse("AI service is unavailable. Please check your OPENAI_API_KEY.",
                    Collections.emptyList());
        }

        // 6. Extract source snippets (first 150 chars of each retrieved chunk)
        List<String> snippets = relevantDocs.stream()
                .map(d -> d.getText().substring(0, Math.min(150, d.getText().length())) + "...")
                .toList();

        log.info("RAG chat answered question for document {} using {} chunks", documentId, relevantDocs.size());
        return new ChatResponse(answer, snippets);
    }

    /**
     * Performs a similarity search in the vector store filtered by documentId.
     */
    private List<Document> searchDocumentChunks(UUID documentId, String query) {
        try {
            return vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(query)
                            .topK(6)
                            .filterExpression("documentId == '" + documentId.toString() + "'")
                            .build()
            );
        } catch (Exception e) {
            log.error("Vector similarity search failed for document {}: {}", documentId, e.getMessage());
            return Collections.emptyList();
        }
    }
}
