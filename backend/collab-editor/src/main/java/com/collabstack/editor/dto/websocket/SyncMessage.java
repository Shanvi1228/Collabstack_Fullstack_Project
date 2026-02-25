package com.collabstack.editor.dto.websocket;

public record SyncMessage(
        String type,      // always "SYNC"
        String content,   // current full document content
        long revision,
        String role        // OWNER, EDITOR, or VIEWER
) {}
