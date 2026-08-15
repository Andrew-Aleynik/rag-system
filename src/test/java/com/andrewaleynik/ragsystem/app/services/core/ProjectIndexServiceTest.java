package com.andrewaleynik.ragsystem.app.services.core;

import com.andrewaleynik.ragsystem.app.dto.request.project.ProjectIndexRequest;
import com.andrewaleynik.ragsystem.app.dto.response.TaskStatusResponse;
import com.andrewaleynik.ragsystem.data.entities.Project;
import com.andrewaleynik.ragsystem.data.repositories.CollectionRepository;
import com.andrewaleynik.ragsystem.data.repositories.ProjectRepository;
import com.andrewaleynik.ragsystem.domains.ProjectType;
import com.andrewaleynik.ragsystem.domains.Task;
import com.andrewaleynik.ragsystem.domains.TaskStatus;
import com.andrewaleynik.ragsystem.domains.TaskType;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectIndexServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private CollectionRepository collectionRepository;
    @Mock
    private IndexService indexService;

    private TaskService taskService;
    private ProjectIndexService projectIndexService;
    private Project project;

    @BeforeEach
    void setUp() {
        taskService = spy(new TaskService(60000, 1));
        AsyncService asyncService = spy(
                new AsyncService(projectRepository, collectionRepository, taskService,
                        null, indexService));
        projectIndexService = new ProjectIndexService(projectRepository, taskService, asyncService);

        project = Project.builder()
                .id(1L)
                .name("test-project")
                .url("https://github.com/test/test.git")
                .type(ProjectType.GIT)
                .defaultBranch("main")
                .build();
    }

    @Test
    void tryStartIndexProject_shouldReturnQueued_whenTaskAccepted() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        doReturn(true).when(taskService).tryAddTask(any(Task.class));

        TaskStatusResponse response = projectIndexService.tryStartIndexProject(new ProjectIndexRequest(1L));

        assertEquals(TaskStatus.QUEUED, response.status());
        verify(taskService).tryAddTask(argThat(task -> task.getType() == TaskType.INDEXING));
    }

    @Test
    void tryStartIndexProject_shouldReturnRejected_whenTaskAlreadyExists() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        doReturn(false).when(taskService).tryAddTask(any(Task.class));

        TaskStatusResponse response = projectIndexService.tryStartIndexProject(new ProjectIndexRequest(1L));

        assertEquals(TaskStatus.REJECTED, response.status());
    }

    @Test
    void tryStartIndexProject_shouldThrowWhenProjectNotFound() {
        when(projectRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                projectIndexService.tryStartIndexProject(new ProjectIndexRequest(1L))
        );
    }
}
