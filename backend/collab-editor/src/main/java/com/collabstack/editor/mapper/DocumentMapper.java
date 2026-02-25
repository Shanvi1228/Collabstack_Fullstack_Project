package com.collabstack.editor.mapper;

import com.collabstack.editor.dto.response.CollaboratorResponse;
import com.collabstack.editor.dto.response.DocumentResponse;
import com.collabstack.editor.entity.Document;
import com.collabstack.editor.entity.DocumentCollaborator;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    @Mapping(source = "owner.username", target = "ownerUsername")
    @Mapping(expression = "java(document.getCollaborators().size())", target = "collaboratorCount")
    @Mapping(target = "userRole", ignore = true)
    DocumentResponse toResponse(Document document);

    List<DocumentResponse> toResponseList(List<Document> documents);

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "user.email", target = "email")
    CollaboratorResponse toCollaboratorResponse(DocumentCollaborator collaborator);
}
