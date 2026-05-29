package com.andrewaleynik.ragsystem.app.dto.request.project;

import jakarta.validation.constraints.NotBlank;

public record ProjectUpdateRequest(
        @NotBlank Long id,
        String name,
        String defaultBranch
) {
}
