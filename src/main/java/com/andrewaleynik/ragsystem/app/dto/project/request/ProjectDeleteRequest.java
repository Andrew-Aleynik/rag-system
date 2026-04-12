package com.andrewaleynik.ragsystem.app.dto.project.request;

import jakarta.validation.constraints.NotBlank;

public record ProjectDeleteRequest(
        @NotBlank Long id
) {
}
