package com.andrewaleynik.ragsystem.app.services;

import com.andrewaleynik.ragsystem.app.dto.project.request.project.*;
import com.andrewaleynik.ragsystem.app.dto.project.response.DocumentListResponse;
import com.andrewaleynik.ragsystem.app.dto.project.response.DocumentResponse;
import com.andrewaleynik.ragsystem.app.dto.project.response.ProjectListResponse;
import com.andrewaleynik.ragsystem.app.dto.project.response.ProjectResponse;
import com.andrewaleynik.ragsystem.config.VectorStoreConfig;
import com.andrewaleynik.ragsystem.data.DocumentData;
import com.andrewaleynik.ragsystem.data.ProjectData;
import com.andrewaleynik.ragsystem.data.entities.ProjectJpaEntity;
import com.andrewaleynik.ragsystem.data.mappers.ProjectMapper;
import com.andrewaleynik.ragsystem.data.repositories.ProjectRepository;
import com.andrewaleynik.ragsystem.domains.ProjectDomain;
import com.andrewaleynik.ragsystem.factories.ProjectFactory;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectCrudService {
    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;
    private final VectorStoreConfig vectorStoreConfig;

    public ProjectResponse createProject(ProjectCreateRequest request) {
        ProjectJpaEntity entity = new ProjectFactory()
                .withUrl(request.url())
                .withDefaultBranch(request.defaultBranch())
                .withName(request.name())
                .withType(request.sourceType())
                .createEntity();
        projectRepository.save(entity);
        return createProjectResponse(entity);
    }

    public ProjectResponse retrieveProject(ProjectRetrieveRequest request) {
        ProjectJpaEntity project = projectRepository.findById(request.id())
                .orElseThrow(() -> new EntityNotFoundException("Project not found: " + request.id()));
        return createProjectResponse(project);
    }

    public ProjectListResponse retrieveProjects() {
        Iterable<ProjectJpaEntity> entities = projectRepository.findAll();
        List<ProjectResponse> projects = StreamSupport.stream(entities.spliterator(), false)
                .map(this::createProjectResponse)
                .toList();
        return new ProjectListResponse(
                projects.size(),
                projects
        );
    }

    @Transactional
    public ProjectResponse updateProject(ProjectUpdateRequest request) {
        ProjectJpaEntity entity = projectRepository.findById(request.id())
                .orElseThrow(() -> new EntityNotFoundException("Project not found: " + request.id()));

        ProjectDomain domain = ProjectFactory.from(entity).createDomain();

        if (request.name() != null) {
            domain.setName(request.name());
        }
        if (request.defaultBranch() != null) {
            domain.setDefaultBranch(request.defaultBranch());
        }

        projectMapper.updateEntity(domain, entity);
        ProjectJpaEntity saved = projectRepository.save(entity);

        return createProjectResponse(saved);
    }

    @Transactional(readOnly = true)
    public DocumentListResponse getProjectDocuments(Long projectId) {
        ProjectJpaEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project " + projectId + " not found"));

        List<DocumentData> documents = project.getDocuments();

        return new DocumentListResponse(
                documents.size(),
                documents.stream()
                        .map(this::createDocumentResponse)
                        .toList()
        );
    }

    public void deleteProject(ProjectDeleteRequest request) {
        ProjectJpaEntity project = projectRepository.findById(request.id())
                .orElseThrow(() -> new EntityNotFoundException("Project not found: " + request.id()));
        vectorStoreConfig.deleteVectorStore(project);
        projectRepository.deleteById(request.id());
    }

    @Transactional
    public void activateProject(ProjectActivateRequest request) {
        ProjectJpaEntity project = projectRepository.findById(request.id())
                .orElseThrow(() -> new EntityNotFoundException("Project not found: " + request.id()));
        project.setActive(true);
        projectRepository.save(project);
    }

    @Transactional
    public void deactivateProject(ProjectDeactivateRequest request) {
        ProjectJpaEntity project = projectRepository.findById(request.id())
                .orElseThrow(() -> new EntityNotFoundException("Project not found: " + request.id()));
        project.setActive(false);
        projectRepository.save(project);
    }

    private ProjectResponse createProjectResponse(ProjectData projectData) {
        return ProjectResponse.builder()
                .id(projectData.getId())
                .url(projectData.getUrl())
                .defaultBranch(projectData.getDefaultBranch())
                .createdAt(projectData.getCreatedAt())
                .updatedAt(projectData.getUpdatedAt())
                .localPath(projectData.getLocalPath())
                .name(projectData.getName())
                .type(projectData.getType())
                .syncedAt(projectData.getSyncedAt())
                .indexedAt(projectData.getIndexedAt())
                .active(projectData.getActive())
                .build();
    }

    private DocumentResponse createDocumentResponse(DocumentData documentData) {
        return DocumentResponse.builder()
                .id(documentData.getId())
                .projectId(documentData.getProjectId())
                .createdAt(documentData.getCreatedAt())
                .updatedAt(documentData.getUpdatedAt())
                .indexedAt(documentData.getIndexedAt())
                .localPath(documentData.getLocalPath())
                .fileName(documentData.getFileName())
                .fileExtension(documentData.getFileExtension())
                .fileHash(documentData.getFileHash())
                .build();
    }
}
