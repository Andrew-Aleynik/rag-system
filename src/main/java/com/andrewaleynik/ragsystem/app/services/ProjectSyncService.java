package com.andrewaleynik.ragsystem.app.services;

import com.andrewaleynik.ragsystem.app.dto.project.request.ProjectSyncRequest;
import com.andrewaleynik.ragsystem.app.dto.project.response.ProjectTaskStatusResponse;
import com.andrewaleynik.ragsystem.data.entities.ProjectJpaEntity;
import com.andrewaleynik.ragsystem.data.repositories.ProjectRepository;
import com.andrewaleynik.ragsystem.domains.Task;
import com.andrewaleynik.ragsystem.domains.TaskStatus;
import com.andrewaleynik.ragsystem.domains.TaskType;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectSyncService {
    private final ProjectRepository projectRepository;
    private final TaskService taskService;
    private final AsyncService asyncService;

    public ProjectTaskStatusResponse tryStartSyncProject(ProjectSyncRequest request) {
        ProjectJpaEntity entity = projectRepository.findById(request.id())
                .orElseThrow(() -> new EntityNotFoundException("Project not found: " + request.id()));


        Task task = Task.builder()
                .type(TaskType.SYNCING)
                .status(TaskStatus.QUEUED)
                .updatedAt(LocalDateTime.now())
                .build();

        if (!taskService.tryAddTask(entity.getId(), task)) {
            return new ProjectTaskStatusResponse(TaskStatus.REJECTED);
        }

        asyncService.syncProject(entity.getId());
        return new ProjectTaskStatusResponse(TaskStatus.QUEUED);
    }
}