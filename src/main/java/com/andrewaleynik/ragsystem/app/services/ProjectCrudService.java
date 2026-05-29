package com.andrewaleynik.ragsystem.app.services;

import com.andrewaleynik.ragsystem.app.dto.request.project.*;
import com.andrewaleynik.ragsystem.app.dto.response.DocumentListResponse;
import com.andrewaleynik.ragsystem.app.dto.response.DocumentResponse;
import com.andrewaleynik.ragsystem.app.dto.response.ProjectListResponse;
import com.andrewaleynik.ragsystem.app.dto.response.ProjectResponse;
import com.andrewaleynik.ragsystem.config.ExVectorStore;
import com.andrewaleynik.ragsystem.config.VectorStoreConfig;
import com.andrewaleynik.ragsystem.data.entities.Chunk;
import com.andrewaleynik.ragsystem.data.entities.Collection;
import com.andrewaleynik.ragsystem.data.entities.Document;
import com.andrewaleynik.ragsystem.data.entities.Project;
import com.andrewaleynik.ragsystem.data.repositories.ChunkRepository;
import com.andrewaleynik.ragsystem.data.repositories.DocumentRepository;
import com.andrewaleynik.ragsystem.data.repositories.ProjectRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectCrudService {
    @Value("${projects.root-path}")
    private String rootPath;
    private final ProjectRepository projectRepository;

    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;
    private final VectorStoreConfig vectorStoreConfig;

    @PostConstruct
    public void init() {
        try {
            Path root = Paths.get(rootPath);
            if (!Files.exists(root)) {
                Files.createDirectories(root);
                log.info("Created projects root directory: {}", rootPath);
            } else {
                log.info("Projects root directory exists: {}", rootPath);
            }
        } catch (IOException e) {
            log.error("Failed to create projects root directory: {}", rootPath, e);
        }
    }

    @Transactional
    public ProjectResponse createProject(ProjectCreateRequest request) {
        String localPath = buildLocalPath(request);
        Project project = Project.builder()
                .url(request.url())
                .defaultBranch(request.defaultBranch())
                .name(request.name())
                .type(request.sourceType())
                .localPath(localPath)
                .build();
        projectRepository.save(project);
        return createProjectResponse(project);
    }

    @Transactional(readOnly = true)
    public ProjectResponse retrieveProject(ProjectRetrieveRequest request) {
        Project project = projectRepository.findById(request.id())
                .orElseThrow(() -> new EntityNotFoundException("Project not found: " + request.id()));
        return createProjectResponse(project);
    }

    @Transactional(readOnly = true)
    public ProjectListResponse retrieveProjects() {
        Iterable<Project> entities = projectRepository.findAll();
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
        Project project = projectRepository.findById(request.id())
                .orElseThrow(() -> new EntityNotFoundException("Project not found: " + request.id()));

        if (request.name() != null) {
            project.setName(request.name());
        }
        if (request.defaultBranch() != null) {
            project.setDefaultBranch(request.defaultBranch());
        }

        Project saved = projectRepository.save(project);

        return createProjectResponse(saved);
    }

    @Transactional(readOnly = true)
    public DocumentListResponse getProjectDocuments(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project " + projectId + " not found"));

        List<Document> documents = project.getDocuments();

        return new DocumentListResponse(
                documents.size(),
                documents.size(),
                documents.stream()
                        .map(this::createDocumentResponse)
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public DocumentListResponse getProjectDocuments(Long projectId, int page, int size) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project " + projectId + " not found"));

        List<Document> documents = project.getDocuments();

        Pageable pageable = PageRequest.of(page, size);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), documents.size());
        Page<Document> documentPage = new PageImpl<>(documents.subList(start, end), pageable, documents.size());

        return new DocumentListResponse(
                documents.size(),
                (int) documentPage.getTotalElements(),
                documentPage.getContent().stream()
                        .map(this::createDocumentResponse)
                        .toList()
        );
    }

    @Transactional
    public void deleteProject(ProjectDeleteRequest request) {
        Project project = projectRepository.findById(request.id())
                .orElseThrow(() -> new EntityNotFoundException("Project not found: " + request.id()));
        deleteVectorsFromCollections(project);
        projectRepository.unlinkDocumentsFromCollections(project.getId());
        vectorStoreConfig.deleteVectorStore(project);
        deleteProjectDirectory(project);
        projectRepository.deleteById(request.id());
    }

    @Transactional
    public void activateProject(ProjectActivateRequest request) {
        Project project = projectRepository.findById(request.id())
                .orElseThrow(() -> new EntityNotFoundException("Project not found: " + request.id()));
        project.setActive(true);
        projectRepository.save(project);
    }

    @Transactional
    public void deactivateProject(ProjectDeactivateRequest request) {
        Project project = projectRepository.findById(request.id())
                .orElseThrow(() -> new EntityNotFoundException("Project not found: " + request.id()));
        project.setActive(false);
        projectRepository.save(project);
    }

    private String buildLocalPath(ProjectCreateRequest request) {
        String path = new StringJoiner("/")
                .add(rootPath)
                .add(request.sourceType().getSourceName())
                .add(sanitizeName(request.name()))
                .toString();

        try {
            Files.createDirectories(Paths.get(path));
            log.info("Created project directory: {}", path);
        } catch (IOException e) {
            log.error("Failed to create project directory: {}", path, e);
            throw new RuntimeException("Cannot create project directory", e);
        }

        return path;
    }

    private String sanitizeName(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\-]", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
    }

    @Transactional(readOnly = true)
    protected ProjectResponse createProjectResponse(Project projectData) {
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
                .documentIds(projectData.getDocuments().stream().map(Document::getId).toList())
                .build();
    }

    private DocumentResponse createDocumentResponse(Document documentData) {
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

    private void deleteVectorsFromCollections(Project project) {
        List<Collection> affectedCollections = projectRepository.findCollections(project.getId());
        List<Document> documents = documentRepository.findAllByProjectId(project.getId());
        List<String> vectorIds = documents.stream()
                .flatMap(doc -> chunkRepository.findAllByDocumentId(doc.getId()).stream())
                .map(Chunk::getVectorId)
                .toList();
        if (vectorIds.isEmpty()) {
            return;
        }
        affectedCollections.forEach(collection -> {
            ExVectorStore collectionVectorStore = vectorStoreConfig.getOrCreateVectorStore(collection);
            collectionVectorStore.vectorStore().delete(vectorIds);
        });
    }

    private void deleteProjectDirectory(Project project) {
        String path = project.getLocalPath();

        Path directoryPath = Paths.get(path);

        try (var stream = Files.walk(directoryPath)) {
            stream.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(java.io.File::delete);
        } catch (IOException e) {
            log.error("Error during directory deletion: ", e);
        }
    }
}