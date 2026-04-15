package com.andrewaleynik.ragsystem.app.dto.project.request.project;

import jakarta.validation.constraints.NotBlank;

public record ProjectIndexRequest(
        @NotBlank Long id
) {
}
