package com.andrewaleynik.ragsystem.app.dto.project.request.collection;

import jakarta.validation.constraints.NotBlank;

public record CollectionIndexServiceRequest(
        @NotBlank Long id
) {
}
