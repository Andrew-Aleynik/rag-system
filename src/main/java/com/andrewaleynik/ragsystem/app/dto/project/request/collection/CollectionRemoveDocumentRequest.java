package com.andrewaleynik.ragsystem.app.dto.project.request.collection;

import java.util.List;

public record CollectionRemoveDocumentRequest(
        List<Long> documentIds
) {
}
