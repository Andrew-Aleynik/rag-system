package com.andrewaleynik.ragsystem.app.dto.project.response;

import java.util.List;

public record DocumentListResponse(
        Integer count,
        List<DocumentResponse> documents
) {
}
