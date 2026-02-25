package com.collabstack.editor.dto.response;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String title,
        String contentSnapshot,
        Long currentRevision,
        String ownerUsername,
        int collaboratorCount,
        String userRole,  // OWNER, EDITOR, or VIEWER
        Instant createdAt,
        Instant updatedAt
) {}
