package com.andrewaleynik.ragsystem.app.dto.response;

import java.util.List;

public record DocumentListResponse(
        Integer totalCount,
        Integer count,
        List<DocumentResponse> documents
) {
}
