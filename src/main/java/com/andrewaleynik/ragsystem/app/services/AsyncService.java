package com.andrewaleynik.ragsystem.app.services;

import com.andrewaleynik.ragsystem.data.entities.ProjectJpaEntity;
import com.andrewaleynik.ragsystem.data.mappers.ProjectMapper;
import com.andrewaleynik.ragsystem.data.repositories.ProjectRepository;
import com.andrewaleynik.ragsystem.domains.ProjectDomain;
import com.andrewaleynik.ragsystem.domains.TaskStatus;
import com.andrewaleynik.ragsystem.domains.TaskType;
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
    private final ProjectMapper projectMapper;
    private final TaskService taskService;
    private final GitRepositoryService gitRepositoryService;
    private final IndexService indexService;

    @Async("threadPoolTaskExecutor")
    @Transactional
    public void syncProject(Long projectId) {
        ProjectJpaEntity entity = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found: " + projectId));
        ProjectDomain domain = ProjectFactory.from(entity).createDomain();
        boolean acquired = false;
        try {
            taskService.acquireSemaphore(TaskType.SYNCING);
            acquired = true;
            taskService.updateStatus(projectId, TaskStatus.IN_PROCESS);
            gitRepositoryService.syncProject(domain);
            gitRepositoryService.updateRepositoryInfo(domain);
            domain.setSyncedAt(LocalDateTime.now());
            saveUpdatedProject(domain, entity);
            taskService.updateStatus(projectId, TaskStatus.DONE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            taskService.updateStatus(projectId, TaskStatus.FAILED);
        } catch (GitAPIException | IOException e) {
            taskService.updateStatus(projectId, TaskStatus.FAILED);
        } finally {
            if (acquired) {
                taskService.releaseSemaphore(TaskType.SYNCING);
            }
        }
    }

    @Async("threadPoolTaskExecutor")
    @Transactional
    public void indexProject(Long projectId) {
        ProjectJpaEntity entity = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found: " + projectId));
        ProjectDomain domain = ProjectFactory.from(entity).createDomain();
        boolean acquired = false;
        try {
            taskService.acquireSemaphore(TaskType.INDEXING);
            acquired = true;
            taskService.updateStatus(projectId, TaskStatus.IN_PROCESS);
            indexService.indexNamedDocumentContainer(domain);
            domain.setIndexedAt(LocalDateTime.now());
            saveUpdatedProject(domain, entity);
            taskService.updateStatus(projectId, TaskStatus.DONE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            taskService.updateStatus(projectId, TaskStatus.FAILED);
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
}
