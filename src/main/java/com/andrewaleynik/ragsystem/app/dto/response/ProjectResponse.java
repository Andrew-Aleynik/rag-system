package com.andrewaleynik.ragsystem.app.dto.response;

import com.andrewaleynik.ragsystem.domains.ProjectType;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

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
        LocalDateTime indexedAt,
        Boolean active,
        List<Long> documentIds
) {
}
