package com.andrewaleynik.ragsystem.app.dto.project.response;

import com.andrewaleynik.ragsystem.domains.TaskStatus;

public record ProjectTaskStatusResponse(
        TaskStatus status
) {
}
