package com.andrewaleynik.ragsystem.app.services.core;

import com.andrewaleynik.ragsystem.app.dto.project.request.project.ProjectIndexRequest;
import com.andrewaleynik.ragsystem.app.dto.project.response.TaskStatusResponse;
import com.andrewaleynik.ragsystem.data.entities.ProjectJpaEntity;
import com.andrewaleynik.ragsystem.data.repositories.ProjectRepository;
import com.andrewaleynik.ragsystem.domains.Task;
import com.andrewaleynik.ragsystem.domains.TaskId;
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

    public TaskStatusResponse tryStartIndexProject(ProjectIndexRequest request) {
        ProjectJpaEntity entity = projectRepository.findById(request.id())
                .orElseThrow(() -> new EntityNotFoundException("Project not found: " + request.id()));

        TaskId taskId = taskService.getTaskId(entity);
        Task task = Task.builder()
                .id(taskId)
                .type(TaskType.INDEXING)
                .status(TaskStatus.QUEUED)
                .updatedAt(LocalDateTime.now())
                .build();

        if (!taskService.tryAddTask(task)) {
            return new TaskStatusResponse(TaskStatus.REJECTED);
        }

        asyncService.indexProject(task.getId());
        return new TaskStatusResponse(TaskStatus.QUEUED);
    }
}
