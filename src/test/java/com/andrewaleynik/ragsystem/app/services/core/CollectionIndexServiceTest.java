package com.andrewaleynik.ragsystem.app.services.core;

import com.andrewaleynik.ragsystem.app.dto.request.collection.CollectionIndexRequest;
import com.andrewaleynik.ragsystem.app.dto.response.TaskStatusResponse;
import com.andrewaleynik.ragsystem.data.entities.Collection;
import com.andrewaleynik.ragsystem.data.repositories.CollectionRepository;
import com.andrewaleynik.ragsystem.data.repositories.ProjectRepository;
import com.andrewaleynik.ragsystem.domains.Task;
import com.andrewaleynik.ragsystem.domains.TaskStatus;
import com.andrewaleynik.ragsystem.domains.TaskType;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CollectionIndexServiceTest {

    @Mock
    private CollectionRepository collectionRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private IndexService indexService;

    private TaskService taskService;
    private CollectionIndexService collectionIndexService;
    private Collection collection;

    @BeforeEach
    void setUp() {
        taskService = spy(new TaskService(60000, 1));
        AsyncService asyncService = spy(
                new AsyncService(projectRepository, collectionRepository, taskService,
                        null, indexService));
        collectionIndexService = new CollectionIndexService(collectionRepository, taskService, asyncService);

        collection = Collection.builder()
                .id(1L)
                .name("test-collection")
                .active(true)
                .documents(new ArrayList<>())
                .build();
    }

    @Test
    void tryStartIndexCollection_shouldReturnQueued_whenTaskAccepted() {
        when(collectionRepository.findById(1L)).thenReturn(Optional.of(collection));
        doReturn(true).when(taskService).tryAddTask(any(Task.class));

        TaskStatusResponse response = collectionIndexService.tryStartIndexCollection(
                new CollectionIndexRequest(1L)
        );

        assertEquals(TaskStatus.QUEUED, response.status());
        verify(taskService).tryAddTask(argThat(task -> task.getType() == TaskType.INDEXING));
    }

    @Test
    void tryStartIndexCollection_shouldReturnRejected_whenTaskAlreadyExists() {
        when(collectionRepository.findById(1L)).thenReturn(Optional.of(collection));
        doReturn(false).when(taskService).tryAddTask(any(Task.class));

        TaskStatusResponse response = collectionIndexService.tryStartIndexCollection(
                new CollectionIndexRequest(1L)
        );

        assertEquals(TaskStatus.REJECTED, response.status());
    }

    @Test
    void tryStartIndexCollection_shouldThrowWhenCollectionNotFound() {
        when(collectionRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                collectionIndexService.tryStartIndexCollection(new CollectionIndexRequest(1L))
        );
    }
}
