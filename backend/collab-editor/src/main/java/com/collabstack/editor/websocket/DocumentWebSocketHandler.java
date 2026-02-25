package com.collabstack.editor.websocket;

import com.collabstack.editor.dto.websocket.CursorMessage;
import com.collabstack.editor.dto.websocket.OperationMessage;
import com.collabstack.editor.dto.websocket.PresenceListMessage;
import com.collabstack.editor.dto.websocket.PresenceMessage;
import com.collabstack.editor.dto.websocket.SyncMessage;
import com.collabstack.editor.entity.CollaboratorRole;
import com.collabstack.editor.entity.Document;
import com.collabstack.editor.repository.DocumentCollaboratorRepository;
import com.collabstack.editor.repository.DocumentRepository;
import com.collabstack.editor.repository.UserRepository;
import com.collabstack.editor.security.JwtTokenProvider;
import com.collabstack.editor.service.OperationPersistenceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentWebSocketHandler extends TextWebSocketHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final DocumentRepository documentRepository;
    private final DocumentCollaboratorRepository collaboratorRepository;
    private final UserRepository userRepository;
    private final CollaborationSessionManager sessionManager;
    private final OperationPersistenceService persistenceService;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        URI uri = session.getUri();
        if (uri == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        // Extract docId from path: /ws/documents/{docId}
        String path = uri.getPath();
        String[] segments = path.split("/");
        if (segments.length < 4) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        String docIdStr = segments[segments.length - 1];

        // Extract token from query param
        String query = uri.getQuery();
        String token = extractQueryParam(query, "token");
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            log.warn("WebSocket rejected: invalid or missing JWT for session {}", session.getId());
            session.close(new CloseStatus(1008, "Invalid or missing authentication token"));
            return;
        }

        UUID userId = jwtTokenProvider.extractUserId(token);
        UUID documentId;
        try {
            documentId = UUID.fromString(docIdStr);
        } catch (IllegalArgumentException e) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        // Check document exists and user has access
        Document document = documentRepository.findById(documentId).orElse(null);
        if (document == null) {
            session.close(new CloseStatus(1008, "Document not found"));
            return;
        }
        boolean isOwner = document.getOwner().getId().equals(userId);
        boolean isCollaborator = collaboratorRepository.existsByDocument_IdAndUser_Id(documentId, userId);
        if (!isOwner && !isCollaborator) {
            session.close(new CloseStatus(1008, "Access denied"));
            return;
        }

        // Retrieve username
        String username = userRepository.findById(userId)
                .map(u -> u.getUsername())
                .orElse("unknown");

        // Determine the user's role for this document
        String userRole;
        if (isOwner) {
            userRole = "OWNER";
        } else {
            userRole = collaboratorRepository.findByDocument_IdAndUser_Id(documentId, userId)
                    .map(collab -> collab.getRole().name())
                    .orElse("VIEWER");
        }

        // Store metadata on the session for later retrieval
        session.getAttributes().put("docId", documentId);
        session.getAttributes().put("userId", userId.toString());
        session.getAttributes().put("username", username);
        session.getAttributes().put("userRole", userRole);

        // Get or create collaborative session
        DocumentSession docSession = sessionManager.getOrCreate(
                documentId, document.getContentSnapshot(), document.getCurrentRevision());
        docSession.addSession(session.getId(), session, userId.toString(), username);

        // Send SYNC to the new client (includes user's role)
        SyncMessage sync = new SyncMessage("SYNC", docSession.getCurrentContent(), docSession.getRevision(), userRole);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(sync)));

        // Send the list of currently connected users to the new client
        List<PresenceListMessage.PresenceUser> userList = docSession.getAllConnectedUsers().stream()
                .map(u -> new PresenceListMessage.PresenceUser(u.userId(), u.username()))
                .toList();
        PresenceListMessage presenceList = new PresenceListMessage("PRESENCE_LIST", userList);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(presenceList)));

        // Broadcast JOIN presence to all OTHER connected clients
        PresenceMessage join = new PresenceMessage("PRESENCE", userId.toString(), username, "JOIN");
        docSession.broadcastToOthers(objectMapper.writeValueAsString(join), session.getId());

        log.info("User {} ({}) joined document {}", username, userId, documentId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        UUID documentId = (UUID) session.getAttributes().get("docId");
        String userId = (String) session.getAttributes().get("userId");
        String username = (String) session.getAttributes().get("username");
        String userRole = (String) session.getAttributes().get("userRole");

        if (documentId == null || userId == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        DocumentSession docSession = sessionManager.get(documentId);
        if (docSession == null) {
            return;
        }

        // Peek at the raw JSON to determine message type
        com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(message.getPayload());
        String messageType = root.has("type") ? root.get("type").asText() : "";

        // Viewers cannot send OPERATION or CURSOR messages
        if ("VIEWER".equals(userRole) && ("OPERATION".equals(messageType) || "CURSOR".equals(messageType))) {
            log.warn("Viewer {} attempted to send {} on document {} — rejected", userId, messageType, documentId);
            return;
        }

        // Handle CURSOR messages — just relay to others
        if ("CURSOR".equals(messageType)) {
            CursorMessage cursor = new CursorMessage(
                    "CURSOR",
                    userId,
                    username,
                    root.has("from") ? root.get("from").asInt() : 0,
                    root.has("to") ? root.get("to").asInt() : 0
            );
            docSession.broadcastToOthers(objectMapper.writeValueAsString(cursor), session.getId());
            return;
        }

        OperationMessage op;
        try {
            op = objectMapper.readValue(message.getPayload(), OperationMessage.class);
        } catch (Exception e) {
            log.warn("Failed to parse OperationMessage from session {}: {}", session.getId(), e.getMessage());
            return;
        }

        // Only handle OPERATION type messages for OT
        if (!"OPERATION".equals(op.type()) || op.opType() == null) {
            return;
        }

        // Apply operation to document session (server-authoritative OT)
        String newContent = docSession.applyOperation(op);
        long newRevision = docSession.getRevision();

        // Persist operation asynchronously
        persistenceService.persistOperation(documentId, UUID.fromString(userId), op, newRevision);

        // Build enriched operation message to broadcast (with server userId/username)
        OperationMessage enriched = new OperationMessage(
                "OPERATION",
                op.opType(),
                op.position(),
                op.content(),
                op.length(),
                newRevision,
                userId,
                username
        );

        // Broadcast to all OTHER sessions
        docSession.broadcastToOthers(objectMapper.writeValueAsString(enriched), session.getId());

        log.debug("Op from {} on doc {}: {} @pos={} rev={}",
                username, documentId, op.opType(), op.position(), newRevision);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        UUID documentId = (UUID) session.getAttributes().get("docId");
        String userId = (String) session.getAttributes().get("userId");
        String username = (String) session.getAttributes().get("username");

        if (documentId == null) {
            return;
        }

        DocumentSession docSession = sessionManager.get(documentId);
        if (docSession == null) {
            return;
        }

        docSession.removeSession(session.getId());

        // Broadcast LEAVE to remaining clients
        if (userId != null && username != null) {
            PresenceMessage leave = new PresenceMessage("PRESENCE", userId, username, "LEAVE");
            docSession.broadcastToAll(objectMapper.writeValueAsString(leave));
        }

        // If last client disconnected: persist snapshot and remove session
        if (docSession.isEmpty()) {
            String finalContent = docSession.getCurrentContent();
            long finalRevision = docSession.getRevision();
            persistenceService.saveSnapshot(documentId, finalContent, finalRevision);
            sessionManager.removeDocumentSession(documentId);
            log.info("Document {} session closed, snapshot saved at revision {}", documentId, finalRevision);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error for session {}: {}", session.getId(), exception.getMessage());
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    private String extractQueryParam(String query, String paramName) {
        if (query == null || query.isEmpty()) {
            return null;
        }
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && paramName.equals(kv[0])) {
                return kv[1];
            }
        }
        return null;
    }
}
