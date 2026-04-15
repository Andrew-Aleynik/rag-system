package com.andrewaleynik.ragsystem.app.services.core;

import com.andrewaleynik.ragsystem.data.entities.CollectionJpaEntity;
import com.andrewaleynik.ragsystem.data.entities.ProjectJpaEntity;
import com.andrewaleynik.ragsystem.data.mappers.CollectionMapper;
import com.andrewaleynik.ragsystem.data.mappers.ProjectMapper;
import com.andrewaleynik.ragsystem.data.repositories.CollectionRepository;
import com.andrewaleynik.ragsystem.data.repositories.ProjectRepository;
import com.andrewaleynik.ragsystem.domains.*;
import com.andrewaleynik.ragsystem.factories.CollectionFactory;
import com.andrewaleynik.ragsystem.factories.ProjectFactory;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AsyncService {
    private final ProjectRepository projectRepository;
    private final CollectionRepository collectionRepository;
    private final ProjectMapper projectMapper;
    private final CollectionMapper collectionMapper;
    private final TaskService taskService;
    private final GitRepositoryService gitRepositoryService;
    private final IndexService indexService;

    @Async("threadPoolTaskExecutor")
    @Transactional
    public void syncProject(TaskId taskId) {
        ProjectJpaEntity entity = projectRepository.findById(taskId.entityId())
                .orElseThrow(() -> new EntityNotFoundException("Project not found: " + taskId.entityId()));
        ProjectDomain domain = ProjectFactory.from(entity).createDomain();
        boolean acquired = false;
        try {
            taskService.acquireSemaphore(TaskType.SYNCING);
            acquired = true;
            taskService.updateStatus(taskId, TaskStatus.IN_PROCESS);
            gitRepositoryService.syncProject(domain);
            gitRepositoryService.updateRepositoryInfo(domain);
            domain.setSyncedAt(LocalDateTime.now());
            saveUpdatedProject(domain, entity);
            taskService.updateStatus(taskId, TaskStatus.DONE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            taskService.updateStatus(taskId, TaskStatus.FAILED);
        } catch (GitAPIException | IOException e) {
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
        ProjectJpaEntity entity = projectRepository.findById(taskId.entityId())
                .orElseThrow(() -> new EntityNotFoundException("Project not found: " + taskId.entityId()));
        ProjectDomain domain = ProjectFactory.from(entity).createDomain();
        boolean acquired = false;
        try {
            taskService.acquireSemaphore(TaskType.INDEXING);
            acquired = true;
            taskService.updateStatus(taskId, TaskStatus.IN_PROCESS);
            indexService.indexNamedDocumentContainer(domain);
            domain.setIndexedAt(LocalDateTime.now());
            saveUpdatedProject(domain, entity);
            taskService.updateStatus(taskId, TaskStatus.DONE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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
        CollectionJpaEntity entity = collectionRepository.findById(taskId.entityId())
                .orElseThrow(() -> new EntityNotFoundException("Collection not found: " + taskId.entityId()));
        CollectionDomain domain = CollectionFactory.from(entity).createDomain();
        boolean acquired = false;
        try {
            taskService.acquireSemaphore(TaskType.INDEXING);
            acquired = true;
            taskService.updateStatus(taskId, TaskStatus.IN_PROCESS);
            indexService.indexNamedDocumentContainer(domain);
            domain.setIndexedAt(LocalDateTime.now());
            saveUpdatedCollection(domain, entity);
            taskService.updateStatus(taskId, TaskStatus.DONE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            taskService.updateStatus(taskId, TaskStatus.FAILED);
        } finally {
            if (acquired) {
                taskService.releaseSemaphore(TaskType.INDEXING);
            }
        }
    }

    private void saveUpdatedProject(ProjectDomain domain, ProjectJpaEntity entity) {
        projectMapper.updateEntity(domain, entity);
        projectRepository.save(entity);
    }

    private void saveUpdatedCollection(CollectionDomain domain, CollectionJpaEntity entity) {
        collectionMapper.updateEntity(domain, entity);
        collectionRepository.save(entity);
    }
}
