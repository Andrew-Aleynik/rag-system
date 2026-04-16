package com.andrewaleynik.ragsystem.app.dto.project.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record CollectionResponse(
        Long id,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime indexedAt,
        String name,
        Boolean active
) {
}
