package com.andrewaleynik.ragsystem.app.services;

import com.andrewaleynik.ragsystem.app.dto.project.request.ProjectIndexServiceRequest;
import com.andrewaleynik.ragsystem.app.dto.project.response.ProjectTaskStatusResponse;
import com.andrewaleynik.ragsystem.data.entities.ProjectJpaEntity;
import com.andrewaleynik.ragsystem.data.repositories.ProjectRepository;
import com.andrewaleynik.ragsystem.domains.Task;
import com.andrewaleynik.ragsystem.domains.TaskStatus;
import com.andrewaleynik.ragsystem.domains.TaskType;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProjectIndexService {
    private final ProjectRepository projectRepository;
    private final TaskService taskService;
    private final AsyncService asyncService;

    public ProjectTaskStatusResponse tryStartIndexProject(ProjectIndexServiceRequest request) {
        ProjectJpaEntity entity = projectRepository.findById(request.id())
                .orElseThrow(() -> new EntityNotFoundException("Project not found: " + request.id()));

        Task task = Task.builder()
                .type(TaskType.INDEXING)
                .status(TaskStatus.QUEUED)
                .updatedAt(LocalDateTime.now())
                .build();

        if (!taskService.tryAddTask(entity.getId(), task)) {
            return new ProjectTaskStatusResponse(TaskStatus.REJECTED);
        }

        asyncService.indexProject(entity.getId());
        return new ProjectTaskStatusResponse(TaskStatus.QUEUED);
    }
}
