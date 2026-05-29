package com.andrewaleynik.ragsystem.app.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record CollectionResponse(
        Long id,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime indexedAt,
        String name,
        Boolean active,
        List<Long> documentIds
) {
}
