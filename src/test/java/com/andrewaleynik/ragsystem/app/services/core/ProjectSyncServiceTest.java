package com.andrewaleynik.ragsystem.app.services.core;

import com.andrewaleynik.ragsystem.app.dto.project.request.project.ProjectSyncRequest;
import com.andrewaleynik.ragsystem.app.dto.project.response.TaskStatusResponse;
import com.andrewaleynik.ragsystem.app.services.core.*;
import com.andrewaleynik.ragsystem.data.entities.ProjectJpaEntity;
import com.andrewaleynik.ragsystem.data.mappers.ProjectMapper;
import com.andrewaleynik.ragsystem.data.repositories.ProjectRepository;
import com.andrewaleynik.ragsystem.domains.ProjectDomain;
import com.andrewaleynik.ragsystem.domains.ProjectType;
import com.andrewaleynik.ragsystem.domains.TaskStatus;
import com.andrewaleynik.ragsystem.domains.TaskType;
import com.andrewaleynik.ragsystem.factories.ProjectFactory;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectSyncServiceTest {
    @Mock
    private ProjectRepository projectRepository;
    private TaskService taskService;
    @Mock
    private GitRepositoryService gitRepositoryService;
    private ProjectSyncService projectSyncService;

    private IndexService indexService;

    private ProjectJpaEntity projectEntity;
    private ProjectSyncRequest syncRequest;

    @BeforeEach
    void setUp() {
        ProjectMapper projectMapper = new ProjectMapper();
        taskService = spy(new TaskService(60000, 1));
        AsyncService asyncService = spy(new AsyncService(projectRepository, projectMapper, taskService, gitRepositoryService, indexService));
        projectSyncService = new ProjectSyncService(
                projectRepository, taskService, asyncService
        );
        projectEntity = new ProjectFactory()
                .withId(1L)
                .withName("test-project")
                .withUrl("https://github.com/test/test.git")
                .withType(ProjectType.GITHUB)
                .withDefaultBranch("master")
                .withLocalPath("/tmp/test")
                .createEntity();

        syncRequest = new ProjectSyncRequest(1L);
    }

    @Test
    void tryStartSyncProject_shouldReturnQueued_whenTaskAccepted() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(projectEntity));
        doReturn(true).when(taskService).tryAddTask(eq(1L), any());

        TaskStatusResponse response = projectSyncService.tryStartSyncProject(syncRequest);

        assertEquals(TaskStatus.QUEUED, response.status());
        verify(projectRepository, atLeastOnce()).findById(1L);
        verify(taskService).tryAddTask(eq(1L), argThat(task -> task.getType() == TaskType.SYNCING));
    }

    @Test
    void tryStartSyncProject_shouldReturnRejected_whenTaskAlreadyExists() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(projectEntity));
        doReturn(false).when(taskService).tryAddTask(eq(1L), any());

        TaskStatusResponse response = projectSyncService.tryStartSyncProject(syncRequest);

        assertEquals(TaskStatus.REJECTED, response.status());
        verify(projectRepository).findById(1L);
        verify(taskService).tryAddTask(anyLong(), any());
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
        doReturn(true).when(taskService).tryAddTask(eq(1L), any());
        doNothing().when(taskService).updateStatus(anyLong(), any());
        doNothing().when(taskService).acquireSemaphore(any());
        doNothing().when(taskService).releaseSemaphore(any());
        doNothing().when(gitRepositoryService).syncProject(any(ProjectDomain.class));
        doNothing().when(gitRepositoryService).updateRepositoryInfo(any(ProjectDomain.class));
        when(projectRepository.save(any(ProjectJpaEntity.class))).thenReturn(projectEntity);

        TaskStatusResponse response = projectSyncService.tryStartSyncProject(syncRequest);

        assertEquals(TaskStatus.QUEUED, response.status());
        verify(taskService, timeout(2000)).updateStatus(1L, TaskStatus.DONE);
        verify(gitRepositoryService, timeout(2000)).syncProject(any(ProjectDomain.class));
        verify(gitRepositoryService, timeout(2000)).updateRepositoryInfo(any(ProjectDomain.class));
        verify(projectRepository, timeout(2000)).save(any(ProjectJpaEntity.class));
        verify(taskService, timeout(2000)).updateStatus(1L, TaskStatus.DONE);
    }
}