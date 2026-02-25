package com.collabstack.editor.repository;

import com.collabstack.editor.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    @Query("SELECT DISTINCT d FROM Document d LEFT JOIN d.collaborators c " +
           "WHERE d.owner.id = :ownerId OR c.user.id = :collaboratorUserId")
    List<Document> findAllByOwnerIdOrCollaborators_UserId(
            @Param("ownerId") UUID ownerId,
            @Param("collaboratorUserId") UUID collaboratorUserId);

    boolean existsByIdAndOwnerId(UUID id, UUID ownerId);

    Optional<Document> findByShareToken(String shareToken);
}
