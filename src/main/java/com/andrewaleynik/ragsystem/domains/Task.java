package com.andrewaleynik.ragsystem.domains;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class Task {
    private TaskType type;
    private TaskStatus status;
    private LocalDateTime updatedAt;
}
