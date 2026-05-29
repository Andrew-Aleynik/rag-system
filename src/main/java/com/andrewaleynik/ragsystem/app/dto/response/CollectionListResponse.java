package com.andrewaleynik.ragsystem.app.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record CollectionListResponse(
        Integer count,
        List<CollectionResponse> collections
) {
}
