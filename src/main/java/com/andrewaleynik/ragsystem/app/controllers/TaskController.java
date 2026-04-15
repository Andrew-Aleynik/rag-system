package com.andrewaleynik.ragsystem.app.controllers;

import com.andrewaleynik.ragsystem.app.dto.project.response.TaskListResponse;
import com.andrewaleynik.ragsystem.app.dto.project.response.TaskResponse;
import com.andrewaleynik.ragsystem.app.services.core.TaskService;
import com.andrewaleynik.ragsystem.domains.Task;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tasks")
@Tag(name = "Task Management", description = "Endpoints for managing tasks")
public class TaskController {
    private final TaskService taskService;

    @GetMapping
    @Operation(summary = "Get all tasks", description = "Retrieves all tasks")
    @ApiResponse(responseCode = "200", description = "Tasks retrieves successfully")
    public ResponseEntity<TaskListResponse> getTasks() {
        List<TaskResponse> tasks = taskService.getTasks().stream()
                .map(this::createTaskResponse)
                .toList();


        return ResponseEntity.ok(new TaskListResponse(tasks.size(), tasks));
    }

    private TaskResponse createTaskResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId().entityClass().getSimpleName() + "_" + task.getId().entityId())
                .type(task.getType())
                .status(task.getStatus())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}
