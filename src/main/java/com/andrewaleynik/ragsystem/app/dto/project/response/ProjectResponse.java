package com.andrewaleynik.ragsystem.app.dto.project.response;

import com.andrewaleynik.ragsystem.domains.ProjectType;
import lombok.Builder;

import java.nio.file.Path;
import java.time.LocalDateTime;

@Builder
public record ProjectResponse(
        Long id,
        String url,
        String defaultBranch,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String localPath,
        Long sizeTokens,
        Long sizeBytes,
        String name,
        ProjectType type,
        LocalDateTime syncedAt,
        LocalDateTime indexedAt
) {
}
