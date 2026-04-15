package com.andrewaleynik.ragsystem.app.services.core;

import com.andrewaleynik.ragsystem.app.dto.project.request.collection.CollectionIndexRequest;
import com.andrewaleynik.ragsystem.app.dto.project.response.TaskStatusResponse;
import com.andrewaleynik.ragsystem.data.entities.CollectionJpaEntity;
import com.andrewaleynik.ragsystem.data.repositories.CollectionRepository;
import com.andrewaleynik.ragsystem.domains.Task;
import com.andrewaleynik.ragsystem.domains.TaskStatus;
import com.andrewaleynik.ragsystem.domains.TaskType;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CollectionIndexService {
    private final CollectionRepository collectionRepository;
    private final TaskService taskService;
    private final AsyncService asyncService;

    public TaskStatusResponse tryStartIndexCollection(CollectionIndexRequest request) {
        CollectionJpaEntity entity = collectionRepository.findById(request.id())
                .orElseThrow(() -> new EntityNotFoundException("Collection not found: " + request.id()));

        Task task = Task.builder()
                .type(TaskType.INDEXING)
                .status(TaskStatus.QUEUED)
                .updatedAt(LocalDateTime.now())
                .build();

        if (!taskService.tryAddTask(entity.getId(), task)) {
            return new TaskStatusResponse(TaskStatus.REJECTED);
        }

        asyncService.indexCollection(entity.getId());
        return new TaskStatusResponse(TaskStatus.QUEUED);
    }
}
