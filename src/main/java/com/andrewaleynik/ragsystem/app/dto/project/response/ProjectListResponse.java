package com.andrewaleynik.ragsystem.app.dto.project.response;

import lombok.Builder;

import java.util.List;

@Builder
public record ProjectListResponse(
        Integer count,
        List<ProjectResponse> projects
) {
}
