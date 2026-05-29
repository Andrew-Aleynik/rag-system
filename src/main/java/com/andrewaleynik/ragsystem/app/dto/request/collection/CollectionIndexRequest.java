package com.andrewaleynik.ragsystem.app.dto.request.collection;

import jakarta.validation.constraints.NotBlank;

public record CollectionIndexRequest(
        @NotBlank Long id
) {
}
