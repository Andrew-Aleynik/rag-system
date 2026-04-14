package com.andrewaleynik.ragsystem.app.dto.project.request.project;

import com.andrewaleynik.ragsystem.domains.ProjectType;
import jakarta.validation.constraints.NotBlank;

public record ProjectCreateRequest(
        @NotBlank String url,
        @NotBlank String defaultBranch,
        @NotBlank String name,
        @NotBlank ProjectType sourceType
) {
}
