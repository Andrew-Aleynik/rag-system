package com.andrewaleynik.ragsystem.app.dto.project.response;

import java.util.List;

public record TaskListResponse(
        Integer count,
        List<TaskResponse> tasks
) {
}
