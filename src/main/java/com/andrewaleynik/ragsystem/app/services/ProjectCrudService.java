package com.andrewaleynik.ragsystem.app.services;

import com.andrewaleynik.ragsystem.app.dto.project.request.project.ProjectCreateRequest;
import com.andrewaleynik.ragsystem.app.dto.project.request.project.ProjectDeleteRequest;
import com.andrewaleynik.ragsystem.app.dto.project.request.project.ProjectRetrieveRequest;
import com.andrewaleynik.ragsystem.app.dto.project.request.project.ProjectUpdateRequest;
import com.andrewaleynik.ragsystem.app.dto.project.response.ProjectListResponse;
import com.andrewaleynik.ragsystem.app.dto.project.response.ProjectResponse;
import com.andrewaleynik.ragsystem.data.ProjectData;
import com.andrewaleynik.ragsystem.data.entities.ProjectJpaEntity;
import com.andrewaleynik.ragsystem.data.mappers.ProjectMapper;
import com.andrewaleynik.ragsystem.data.repositories.ProjectRepository;
import com.andrewaleynik.ragsystem.domains.ProjectDomain;
import com.andrewaleynik.ragsystem.factories.ProjectFactory;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class ProjectCrudService {
    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;

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

    public ProjectListResponse retrieveProjects(ProjectRetrieveRequest request) {
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

    public void deleteProject(ProjectDeleteRequest request) {
        if (!projectRepository.existsById(request.id())) {
            throw new EntityNotFoundException("Project not found: " + request.id());
        }
        projectRepository.deleteById(request.id());
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
                .build();
    }
}
