package com.collabstack.editor.service.impl;

import com.collabstack.editor.dto.request.AddCollaboratorRequest;
import com.collabstack.editor.dto.request.DocumentCreateRequest;
import com.collabstack.editor.dto.response.CollaboratorResponse;
import com.collabstack.editor.dto.response.DocumentResponse;
import com.collabstack.editor.entity.Document;
import com.collabstack.editor.entity.DocumentCollaborator;
import com.collabstack.editor.entity.CollaboratorRole;
import com.collabstack.editor.entity.User;
import com.collabstack.editor.exception.ConflictException;
import com.collabstack.editor.exception.ResourceNotFoundException;
import com.collabstack.editor.exception.UnauthorizedException;
import com.collabstack.editor.mapper.DocumentMapper;
import com.collabstack.editor.repository.DocumentCollaboratorRepository;
import com.collabstack.editor.repository.DocumentRepository;
import com.collabstack.editor.repository.UserRepository;
import com.collabstack.editor.service.DocumentService;
import com.collabstack.editor.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentCollaboratorRepository collaboratorRepository;
    private final UserRepository userRepository;
    private final DocumentMapper documentMapper;

    // Optional — only injected when EmbeddingService bean is available
    @Autowired(required = false)
    private EmbeddingService embeddingService;

    @Override
    @Transactional
    public DocumentResponse create(UUID userId, DocumentCreateRequest request) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        Document document = Document.builder()
                .owner(owner)
                .title(request.title())
                .contentSnapshot(request.initialContent() != null ? request.initialContent() : "")
                .currentRevision(0L)
                .build();
        Document saved = documentRepository.save(document);
        // Async index initial content if non-empty
        if (embeddingService != null && saved.getContentSnapshot() != null && !saved.getContentSnapshot().isBlank()) {
            embeddingService.indexDocument(saved.getId(), saved.getContentSnapshot());
        }
        return documentMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentResponse findById(UUID documentId, UUID requestingUserId) {
        Document document = getDocumentOrThrow(documentId);
        assertAccess(document, requestingUserId);
        DocumentResponse response = documentMapper.toResponse(document);
        String userRole = resolveUserRole(document, requestingUserId);
        return new DocumentResponse(
                response.id(), response.title(), response.contentSnapshot(),
                response.currentRevision(), response.ownerUsername(),
                response.collaboratorCount(), userRole,
                response.createdAt(), response.updatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> findAllForUser(UUID userId) {
        List<Document> documents = documentRepository
                .findAllByOwnerIdOrCollaborators_UserId(userId, userId);
        return documentMapper.toResponseList(documents);
    }

    @Override
    @Transactional
    public DocumentResponse updateTitle(UUID documentId, UUID userId, String newTitle) {
        Document document = getDocumentOrThrow(documentId);
        assertOwner(document, userId);
        document.setTitle(newTitle);
        return documentMapper.toResponse(documentRepository.save(document));
    }

    @Override
    @Transactional
    public void delete(UUID documentId, UUID userId) {
        Document document = getDocumentOrThrow(documentId);
        assertOwner(document, userId);
        documentRepository.delete(document);
        // Clean up vector store chunks async
        if (embeddingService != null) {
            embeddingService.deleteDocumentChunks(documentId);
        }
    }

    @Override
    @Transactional
    public CollaboratorResponse addCollaborator(UUID documentId, UUID ownerUserId,
                                                AddCollaboratorRequest request) {
        Document document = getDocumentOrThrow(documentId);
        assertOwner(document, ownerUserId);

        User collaboratorUser = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No user found with email: " + request.email()));

        if (collaboratorUser.getId().equals(ownerUserId)) {
            throw new ConflictException("Owner cannot be added as a collaborator");
        }

        if (collaboratorRepository.existsByDocument_IdAndUser_Id(documentId, collaboratorUser.getId())) {
            throw new ConflictException("User is already a collaborator on this document");
        }

        DocumentCollaborator collaborator = DocumentCollaborator.builder()
                .document(document)
                .user(collaboratorUser)
                .role(request.role())
                .build();
        DocumentCollaborator saved = collaboratorRepository.save(collaborator);
        return documentMapper.toCollaboratorResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CollaboratorResponse> getCollaborators(UUID documentId, UUID userId) {
        Document document = getDocumentOrThrow(documentId);
        assertAccess(document, userId);
        List<DocumentCollaborator> collaborators = collaboratorRepository.findAllByDocument_Id(documentId);
        return collaborators.stream().map(documentMapper::toCollaboratorResponse).toList();
    }

    @Override
    @Transactional
    public String generateShareToken(UUID documentId, UUID ownerUserId) {
        Document document = getDocumentOrThrow(documentId);
        assertOwner(document, ownerUserId);
        if (document.getShareToken() == null || document.getShareToken().isBlank()) {
            String token = UUID.randomUUID().toString().replace("-", "");
            document.setShareToken(token);
            documentRepository.save(document);
        }
        return document.getShareToken();
    }

    @Override
    @Transactional
    public DocumentResponse joinViaShareToken(String shareToken, UUID userId) {
        Document document = documentRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired share link"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        // Owner is already a member
        if (document.getOwner().getId().equals(userId)) {
            return documentMapper.toResponse(document);
        }

        // Already a collaborator — just return doc
        if (collaboratorRepository.existsByDocument_IdAndUser_Id(document.getId(), userId)) {
            return documentMapper.toResponse(document);
        }

        // Add as EDITOR collaborator
        DocumentCollaborator collaborator = DocumentCollaborator.builder()
                .document(document)
                .user(user)
                .role(CollaboratorRole.EDITOR)
                .build();
        collaboratorRepository.save(collaborator);
        log.info("User {} joined document {} via share token", userId, document.getId());
        return documentMapper.toResponse(document);
    }

    // --- helpers ---

    private Document getDocumentOrThrow(UUID documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
    }

    private void assertOwner(Document document, UUID userId) {
        if (!document.getOwner().getId().equals(userId)) {
            throw new UnauthorizedException("Only the document owner can perform this action");
        }
    }

    private void assertAccess(Document document, UUID userId) {
        boolean isOwner = document.getOwner().getId().equals(userId);
        boolean isCollaborator = collaboratorRepository
                .existsByDocument_IdAndUser_Id(document.getId(), userId);
        if (!isOwner && !isCollaborator) {
            throw new UnauthorizedException("You do not have access to this document");
        }
    }

    private String resolveUserRole(Document document, UUID userId) {
        if (document.getOwner().getId().equals(userId)) {
            return "OWNER";
        }
        return collaboratorRepository.findByDocument_IdAndUser_Id(document.getId(), userId)
                .map(collab -> collab.getRole().name())
                .orElse("VIEWER");
    }
}
