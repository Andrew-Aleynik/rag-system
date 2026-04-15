package com.andrewaleynik.ragsystem.app.dto.project.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record DocumentResponse(
        Long id,
        Long projectId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime indexedAt,
        String localPath,
        String fileName,
        String fileExtension,
        String fileHash
) {
}
