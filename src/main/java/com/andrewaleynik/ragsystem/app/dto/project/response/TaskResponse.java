package com.andrewaleynik.ragsystem.app.dto.project.response;

import com.andrewaleynik.ragsystem.domains.TaskStatus;
import com.andrewaleynik.ragsystem.domains.TaskType;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record TaskResponse(
        String id,
        TaskType type,
        TaskStatus status,
        LocalDateTime updatedAt
) {
}
