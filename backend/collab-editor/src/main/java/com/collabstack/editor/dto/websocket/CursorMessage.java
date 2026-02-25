package com.collabstack.editor.dto.websocket;

public record CursorMessage(
        String type,      // always "CURSOR"
        String userId,
        String username,
        int from,         // cursor position start
        int to            // cursor position end (if from != to, it's a selection)
) {}
