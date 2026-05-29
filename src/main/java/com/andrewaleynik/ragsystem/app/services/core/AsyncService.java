package com.andrewaleynik.ragsystem.app.services.core;

import com.andrewaleynik.ragsystem.data.entities.Collection;
import com.andrewaleynik.ragsystem.data.entities.Project;
import com.andrewaleynik.ragsystem.data.repositories.CollectionRepository;
import com.andrewaleynik.ragsystem.data.repositories.ProjectRepository;
import com.andrewaleynik.ragsystem.domains.Task;
import com.andrewaleynik.ragsystem.domains.TaskId;
import com.andrewaleynik.ragsystem.domains.TaskStatus;
import com.andrewaleynik.ragsystem.domains.TaskType;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncService {
    private final ProjectRepository projectRepository;
    private final CollectionRepository collectionRepository;
    private final TaskService taskService;
    private final GitRepositoryService gitRepositoryService;
    private final IndexService indexService;

    @Async("threadPoolTaskExecutor")
    @Transactional
    public void syncProject(TaskId taskId) {
        Project project = projectRepository.findById(taskId.entityId())
                .orElseThrow(() -> new EntityNotFoundException("Project not found: " + taskId.entityId()));
        boolean acquired = false;
        try {
            taskService.acquireSemaphore(TaskType.SYNCING);
            acquired = true;
            taskService.updateStatus(taskId, TaskStatus.IN_PROCESS);
            Task task = taskService.getTask(taskId).get();
            gitRepositoryService.syncProject(project, task.getUsername(), task.getPassword());
            gitRepositoryService.updateRepositoryInfo(project);
            project.setSyncedAt(LocalDateTime.now());
            projectRepository.save(project);
            taskService.updateStatus(taskId, TaskStatus.DONE);
        } catch (InterruptedException e) {
            log.error("Error during syncing project: ", e);
            Thread.currentThread().interrupt();
            taskService.updateStatus(taskId, TaskStatus.FAILED);
        } catch (GitAPIException | IOException e) {
            log.error("Error during syncing project: ", e);
            taskService.updateStatus(taskId, TaskStatus.FAILED);
        } finally {
            if (acquired) {
                taskService.releaseSemaphore(TaskType.SYNCING);
            }
        }
    }

    @Async("threadPoolTaskExecutor")
    @Transactional
    public void indexProject(TaskId taskId) {
        Project project = projectRepository.findById(taskId.entityId())
                .orElseThrow(() -> new EntityNotFoundException("Project not found: " + taskId.entityId()));
        boolean acquired = false;
        try {
            taskService.acquireSemaphore(TaskType.INDEXING);
            acquired = true;
            taskService.updateStatus(taskId, TaskStatus.IN_PROCESS);
            indexService.indexNamedDocumentContainer(project);
            project.setIndexedAt(LocalDateTime.now());
            projectRepository.save(project);
            taskService.updateStatus(taskId, TaskStatus.DONE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            taskService.updateStatus(taskId, TaskStatus.FAILED);
        } catch (RuntimeException e) {
            log.error("Error during indexing project: ", e);
            taskService.updateStatus(taskId, TaskStatus.FAILED);
        } finally {
            if (acquired) {
                taskService.releaseSemaphore(TaskType.INDEXING);
            }
        }
    }

    @Async("threadPoolTaskExecutor")
    @Transactional
    public void indexCollection(TaskId taskId) {
        Collection collection = collectionRepository.findById(taskId.entityId())
                .orElseThrow(() -> new EntityNotFoundException("Collection not found: " + taskId.entityId()));
        boolean acquired = false;
        try {
            taskService.acquireSemaphore(TaskType.INDEXING);
            acquired = true;
            taskService.updateStatus(taskId, TaskStatus.IN_PROCESS);
            indexService.indexNamedDocumentContainer(collection);
            collection.setIndexedAt(LocalDateTime.now());
            collectionRepository.save(collection);
            taskService.updateStatus(taskId, TaskStatus.DONE);
        } catch (InterruptedException e) {
            log.error("Error during indexing collection: ", e);
            Thread.currentThread().interrupt();
            taskService.updateStatus(taskId, TaskStatus.FAILED);
        } catch (RuntimeException e) {
            log.error("Error during indexing collection: ", e);
            taskService.updateStatus(taskId, TaskStatus.FAILED);
        } finally {
            if (acquired) {
                taskService.releaseSemaphore(TaskType.INDEXING);
            }
        }
    }
}
