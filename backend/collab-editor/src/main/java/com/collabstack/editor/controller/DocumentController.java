package com.collabstack.editor.controller;

import com.collabstack.editor.dto.request.AddCollaboratorRequest;
import com.collabstack.editor.dto.request.DocumentCreateRequest;
import com.collabstack.editor.dto.request.UpdateTitleRequest;
import com.collabstack.editor.dto.response.CollaboratorResponse;
import com.collabstack.editor.dto.response.DocumentResponse;
import com.collabstack.editor.exception.ApiResponse;
import com.collabstack.editor.security.UserPrincipal;
import com.collabstack.editor.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Document CRUD and collaborator management")
@SecurityRequirement(name = "bearerAuth")
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping
    @Operation(summary = "List all documents for the authenticated user")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> findAll(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<DocumentResponse> documents = documentService.findAllForUser(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(documents));
    }

    @PostMapping
    @Operation(summary = "Create a new document")
    public ResponseEntity<ApiResponse<DocumentResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody DocumentCreateRequest request) {
        DocumentResponse response = documentService.create(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Document created", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a document by ID")
    public ResponseEntity<ApiResponse<DocumentResponse>> findById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        DocumentResponse response = documentService.findById(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/{id}/title")
    @Operation(summary = "Update document title (owner only)")
    public ResponseEntity<ApiResponse<DocumentResponse>> updateTitle(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateTitleRequest request) {
        DocumentResponse response = documentService.updateTitle(id, principal.getId(), request.title());
        return ResponseEntity.ok(ApiResponse.ok("Title updated", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a document (owner only)")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        documentService.delete(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Document deleted", null));
    }

    @PostMapping("/{id}/collaborators")
    @Operation(summary = "Add a collaborator to a document (owner only)")
    public ResponseEntity<ApiResponse<CollaboratorResponse>> addCollaborator(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddCollaboratorRequest request) {
        CollaboratorResponse response = documentService.addCollaborator(id, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Collaborator added", response));
    }

    @GetMapping("/{id}/collaborators")
    @Operation(summary = "List collaborators for a document")
    public ResponseEntity<ApiResponse<List<CollaboratorResponse>>> getCollaborators(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<CollaboratorResponse> collaborators = documentService.getCollaborators(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(collaborators));
    }

    @PostMapping("/{id}/share")
    @Operation(summary = "Generate a share token for a document (owner only)")
    public ResponseEntity<ApiResponse<String>> generateShareToken(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        String token = documentService.generateShareToken(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Share token generated", token));
    }

    @PostMapping("/join/{shareToken}")
    @Operation(summary = "Join a document via share token")
    public ResponseEntity<ApiResponse<DocumentResponse>> joinViaShareToken(
            @PathVariable String shareToken,
            @AuthenticationPrincipal UserPrincipal principal) {
        DocumentResponse response = documentService.joinViaShareToken(shareToken, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Joined document", response));
    }
}
