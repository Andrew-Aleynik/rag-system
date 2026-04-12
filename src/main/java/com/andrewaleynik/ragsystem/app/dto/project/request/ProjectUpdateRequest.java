package com.andrewaleynik.ragsystem.app.dto.project.request;

import jakarta.validation.constraints.NotBlank;

public record ProjectUpdateRequest(
        @NotBlank Long id,
        String name,
        String defaultBranch
) {
}
