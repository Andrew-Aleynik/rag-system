package com.andrewaleynik.ragsystem.app.dto.response;

import com.andrewaleynik.ragsystem.domains.TaskStatus;

public record TaskStatusResponse(
        TaskStatus status
) {
}
