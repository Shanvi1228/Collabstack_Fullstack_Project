package com.collabstack.editor.websocket;

import com.collabstack.editor.dto.websocket.OperationMessage;
import com.collabstack.editor.dto.websocket.OperationType;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class DocumentSession {

    @Getter
    private final UUID documentId;

    private volatile String currentContent;

    @Getter
    private volatile long revision;

    // sessionId -> WebSocketSession
    private final ConcurrentHashMap<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();

    // sessionId -> UserInfo
    private final ConcurrentHashMap<String, UserInfo> sessionUsers = new ConcurrentHashMap<>();

    public record UserInfo(String userId, String username) {}

    public DocumentSession(UUID documentId, String initialContent, long revision) {
        this.documentId = documentId;
        this.currentContent = initialContent != null ? initialContent : "";
        this.revision = revision;
    }

    /**
     * Applies an operation to the document content.
     * Server-authoritative OT: for MVP, applies ops as-is regardless of revision delta.
     * Full OT transform against missed ops is a future improvement.
     *
     * @return the new document content after apply
     */
    public synchronized String applyOperation(OperationMessage op) {
        if (op.opType() == OperationType.REPLACE && op.content() != null) {
            // Full content replacement — used by the rich-text editor
            currentContent = op.content();
        } else if (op.opType() == OperationType.INSERT && op.content() != null) {
            int pos = Math.min(Math.max(op.position(), 0), currentContent.length());
            currentContent = currentContent.substring(0, pos)
                    + op.content()
                    + currentContent.substring(pos);
        } else if (op.opType() == OperationType.DELETE) {
            int pos = Math.min(Math.max(op.position(), 0), currentContent.length());
            int end = Math.min(pos + op.length(), currentContent.length());
            if (pos < end) {
                currentContent = currentContent.substring(0, pos) + currentContent.substring(end);
            }
        }
        revision++;
        return currentContent;
    }

    public synchronized String getCurrentContent() {
        return currentContent;
    }

    public void addSession(String sessionId, WebSocketSession ws, String userId, String username) {
        activeSessions.put(sessionId, ws);
        sessionUsers.put(sessionId, new UserInfo(userId, username));
    }

    public void removeSession(String sessionId) {
        activeSessions.remove(sessionId);
        sessionUsers.remove(sessionId);
    }

    public boolean isEmpty() {
        return activeSessions.isEmpty();
    }

    public UserInfo getUserInfo(String sessionId) {
        return sessionUsers.get(sessionId);
    }

    /**
     * Returns all currently connected users (de-duplicated by userId).
     */
    public List<UserInfo> getAllConnectedUsers() {
        return sessionUsers.values().stream()
                .distinct()
                .toList();
    }

    /**
     * Broadcasts a JSON message to all sessions EXCEPT the sender.
     */
    public void broadcastToOthers(String json, String senderSessionId) {
        activeSessions.forEach((sid, ws) -> {
            if (!sid.equals(senderSessionId) && ws.isOpen()) {
                try {
                    ws.sendMessage(new TextMessage(json));
                } catch (IOException e) {
                    log.warn("Failed to send message to session {}: {}", sid, e.getMessage());
                }
            }
        });
    }

    /**
     * Broadcasts a JSON message to ALL connected sessions.
     */
    public void broadcastToAll(String json) {
        activeSessions.forEach((sid, ws) -> {
            if (ws.isOpen()) {
                try {
                    ws.sendMessage(new TextMessage(json));
                } catch (IOException e) {
                    log.warn("Failed to broadcast to session {}: {}", sid, e.getMessage());
                }
            }
        });
    }
}
