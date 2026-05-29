package com.andrewaleynik.ragsystem.app.dto.request.collection;

import jakarta.validation.constraints.NotBlank;

public record CollectionSyncRequest(
        @NotBlank Long id
) {
}
