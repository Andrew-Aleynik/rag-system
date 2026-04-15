package com.andrewaleynik.ragsystem.app.controllers;

import com.andrewaleynik.ragsystem.app.dto.project.request.project.*;
import com.andrewaleynik.ragsystem.app.dto.project.response.ProjectListResponse;
import com.andrewaleynik.ragsystem.app.dto.project.response.ProjectResponse;
import com.andrewaleynik.ragsystem.app.dto.project.response.TaskStatusResponse;
import com.andrewaleynik.ragsystem.app.services.ProjectCrudService;
import com.andrewaleynik.ragsystem.app.services.core.ProjectIndexService;
import com.andrewaleynik.ragsystem.app.services.core.ProjectSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects")
@Tag(name = "Project Management", description = "Endpoints for managing projects")
public class ProjectController {

    private final ProjectCrudService projectCrudService;
    private final ProjectSyncService projectSyncService;
    private final ProjectIndexService projectIndexService;

    @PostMapping
    @Operation(summary = "Create a new project", description = "Creates a new project with the provided details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Project created successfully",
                    content = @Content(schema = @Schema(implementation = ProjectResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "409", description = "Project already exists")
    })
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody ProjectCreateRequest request
    ) {
        log.info("Creating new project with name: {}", request.name());
        ProjectResponse response = projectCrudService.createProject(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all projects", description = "Retrieves a list of all projects")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Projects retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ProjectListResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ProjectListResponse> getAllProjects(
            @Valid ProjectRetrieveRequest request
    ) {
        log.info("Retrieving all projects");
        ProjectListResponse response = projectCrudService.retrieveProjects(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get project by ID", description = "Retrieves a single project by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Project found",
                    content = @Content(schema = @Schema(implementation = ProjectResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found")
    })
    public ResponseEntity<ProjectResponse> getProjectById(
            @Parameter(description = "Project ID", required = true)
            @PathVariable Long id
    ) {
        log.info("Retrieving project with id: {}", id);
        ProjectRetrieveRequest request = new ProjectRetrieveRequest();
        ProjectListResponse response = projectCrudService.retrieveProjects(request);

        if (response.projects().isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(response.projects().get(0));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update project", description = "Updates an existing project")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Project updated successfully",
                    content = @Content(schema = @Schema(implementation = ProjectResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "404", description = "Project not found")
    })
    public ResponseEntity<ProjectResponse> updateProject(
            @Parameter(description = "Project ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody ProjectUpdateRequest request
    ) {
        log.info("Updating project with id: {}", id);
        ProjectUpdateRequest updateRequest = new ProjectUpdateRequest(
                id,
                request.name(),
                request.defaultBranch()
        );
        ProjectResponse response = projectCrudService.updateProject(updateRequest);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete project", description = "Deletes a project by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Project deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Project not found")
    })
    public ResponseEntity<Void> deleteProject(
            @Parameter(description = "Project ID", required = true)
            @PathVariable Long id
    ) {
        log.info("Deleting project with id: {}", id);
        ProjectDeleteRequest request = new ProjectDeleteRequest(id);
        projectCrudService.deleteProject(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/sync")
    @Operation(summary = "Sync project", description = "Starts synchronization of project files")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Sync task queued successfully",
                    content = @Content(schema = @Schema(implementation = TaskStatusResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found"),
            @ApiResponse(responseCode = "409", description = "Sync already in progress")
    })
    public ResponseEntity<TaskStatusResponse> syncProject(
            @Parameter(description = "Project ID", required = true)
            @PathVariable Long id
    ) {
        log.info("Starting sync for project with id: {}", id);
        ProjectSyncRequest request = new ProjectSyncRequest(id);
        TaskStatusResponse response = projectSyncService.tryStartSyncProject(request);

        HttpStatus status = switch (response.status()) {
            case QUEUED -> HttpStatus.ACCEPTED;
            case REJECTED -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };

        return ResponseEntity.status(status).body(response);
    }

    @PostMapping("/{id}/index")
    @Operation(summary = "Index project", description = "Starts indexing of project files for RAG")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Index task queued successfully",
                    content = @Content(schema = @Schema(implementation = TaskStatusResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found"),
            @ApiResponse(responseCode = "409", description = "Index already in progress")
    })
    public ResponseEntity<TaskStatusResponse> indexProject(
            @Parameter(description = "Project ID", required = true)
            @PathVariable Long id
    ) {
        log.info("Starting indexing for project with id: {}", id);
        ProjectIndexRequest request = new ProjectIndexRequest(id);
        TaskStatusResponse response = projectIndexService.tryStartIndexProject(request);

        HttpStatus status = switch (response.status()) {
            case QUEUED -> HttpStatus.ACCEPTED;
            case REJECTED -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };

        return ResponseEntity.status(status).body(response);
    }
}