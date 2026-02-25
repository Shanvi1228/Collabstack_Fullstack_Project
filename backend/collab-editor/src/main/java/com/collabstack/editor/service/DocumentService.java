package com.collabstack.editor.service;

import com.collabstack.editor.dto.request.AddCollaboratorRequest;
import com.collabstack.editor.dto.request.DocumentCreateRequest;
import com.collabstack.editor.dto.response.CollaboratorResponse;
import com.collabstack.editor.dto.response.DocumentResponse;

import java.util.List;
import java.util.UUID;

public interface DocumentService {
    DocumentResponse create(UUID userId, DocumentCreateRequest request);
    DocumentResponse findById(UUID documentId, UUID requestingUserId);
    List<DocumentResponse> findAllForUser(UUID userId);
    DocumentResponse updateTitle(UUID documentId, UUID userId, String newTitle);
    void delete(UUID documentId, UUID userId);
    CollaboratorResponse addCollaborator(UUID documentId, UUID ownerUserId, AddCollaboratorRequest request);
    List<CollaboratorResponse> getCollaborators(UUID documentId, UUID userId);
    String generateShareToken(UUID documentId, UUID ownerUserId);
    DocumentResponse joinViaShareToken(String shareToken, UUID userId);
}
