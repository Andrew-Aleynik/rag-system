package com.andrewaleynik.ragsystem.app.dto.response;

import java.util.List;

public record TaskListResponse(
        Integer count,
        List<TaskResponse> tasks
) {
}
