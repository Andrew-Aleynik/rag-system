package com.andrewaleynik.ragsystem.app.dto.request.collection;

import java.util.List;

public record CollectionRemoveDocumentRequest(
        List<Long> documentIds
) {
}
