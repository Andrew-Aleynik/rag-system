package com.andrewaleynik.ragsystem.app.dto.request.project;

import jakarta.validation.constraints.NotBlank;

public record ProjectDeleteRequest(
        @NotBlank Long id
) {
}
