package com.andrewaleynik.ragsystem.app.dto.request.project;

import jakarta.validation.constraints.NotBlank;

public record ProjectSyncRequest(
        @NotBlank Long id,
        String username,
        String password
) {
}
