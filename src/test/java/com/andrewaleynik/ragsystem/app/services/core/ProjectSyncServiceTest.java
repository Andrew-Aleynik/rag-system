package com.andrewaleynik.ragsystem.app.services.core;

import com.andrewaleynik.ragsystem.app.dto.request.project.ProjectSyncRequest;
import com.andrewaleynik.ragsystem.app.dto.response.TaskStatusResponse;
import com.andrewaleynik.ragsystem.data.entities.Project;
import com.andrewaleynik.ragsystem.data.repositories.CollectionRepository;
import com.andrewaleynik.ragsystem.data.repositories.ProjectRepository;
import com.andrewaleynik.ragsystem.domains.*;
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
class ProjectSyncServiceTest {
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private CollectionRepository collectionRepository;
    private TaskService taskService;
    @Mock
    private GitRepositoryService gitRepositoryService;
    private ProjectSyncService projectSyncService;
    private Project projectEntity;
    private ProjectSyncRequest syncRequest;

    @BeforeEach
    void setUp() {
        taskService = spy(new TaskService(60000, 1));
        AsyncService asyncService = spy(
                new AsyncService(projectRepository, collectionRepository, taskService,
                        gitRepositoryService, null));
        projectSyncService = new ProjectSyncService(
                projectRepository, taskService, asyncService
        );
        projectEntity = Project.builder()
                .id(1L)
                .name("test-project")
                .url("https://github.com/test/test.git")
                .type(ProjectType.GIT)
                .defaultBranch("master")
                .localPath("/tmp/test")
                .build();

        syncRequest = new ProjectSyncRequest(1L, null, null);
    }

    @Test
    void tryStartSyncProject_shouldReturnQueued_whenTaskAccepted() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(projectEntity));
        doReturn(true).when(taskService).tryAddTask(any(Task.class));
        doReturn(Optional.of(mock(Task.class))).when(taskService).getTask(any());

        TaskStatusResponse response = projectSyncService.tryStartSyncProject(syncRequest);

        assertEquals(TaskStatus.QUEUED, response.status());
        verify(projectRepository, atLeastOnce()).findById(1L);
        verify(taskService).tryAddTask(argThat(task -> task.getType() == TaskType.SYNCING));
    }

    @Test
    void tryStartSyncProject_shouldReturnRejected_whenTaskAlreadyExists() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(projectEntity));
        doReturn(false).when(taskService).tryAddTask(any(Task.class));

        TaskStatusResponse response = projectSyncService.tryStartSyncProject(syncRequest);

        assertEquals(TaskStatus.REJECTED, response.status());
        verify(projectRepository).findById(1L);
        verify(taskService).tryAddTask(any(Task.class));
    }

    @Test
    void tryStartSyncProject_shouldThrowException_whenProjectNotFound() {
        when(projectRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                projectSyncService.tryStartSyncProject(syncRequest)
        );
        verify(projectRepository).findById(1L);
        verifyNoInteractions(taskService);
    }

    @Test
    void tryStartSyncProject_shouldCompleteAsyncSyncSuccessfully() throws Exception {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(projectEntity));
        doReturn(true).when(taskService).tryAddTask(any(Task.class));
        doNothing().when(taskService).updateStatus(any(), any());
        doNothing().when(taskService).acquireSemaphore(any());
        doNothing().when(taskService).releaseSemaphore(any());
        doReturn(Optional.of(mock(Task.class))).when(taskService).getTask(any());
        doNothing().when(gitRepositoryService).syncProject(any(Project.class), nullable(String.class), nullable(String.class));
        doNothing().when(gitRepositoryService).updateRepositoryInfo(any(Project.class));
        when(projectRepository.save(any(Project.class))).thenReturn(projectEntity);

        TaskStatusResponse response = projectSyncService.tryStartSyncProject(syncRequest);

        assertEquals(TaskStatus.QUEUED, response.status());
        verify(taskService, timeout(2000)).updateStatus(new TaskId(Project.class, 1L), TaskStatus.DONE);
        verify(gitRepositoryService, timeout(2000)).syncProject(any(Project.class), nullable(String.class), nullable(String.class));
        verify(gitRepositoryService, timeout(2000)).updateRepositoryInfo(any(Project.class));
        verify(projectRepository, timeout(2000)).save(any(Project.class));
        verify(taskService, timeout(2000)).updateStatus(new TaskId(Project.class, 1L), TaskStatus.DONE);
    }
}