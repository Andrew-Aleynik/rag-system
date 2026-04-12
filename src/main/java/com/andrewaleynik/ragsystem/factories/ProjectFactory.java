package com.andrewaleynik.ragsystem.factories;

import com.andrewaleynik.ragsystem.data.DocumentData;
import com.andrewaleynik.ragsystem.data.ProjectData;
import com.andrewaleynik.ragsystem.data.entities.ProjectJpaEntity;
import com.andrewaleynik.ragsystem.domains.ProjectDomain;
import com.andrewaleynik.ragsystem.domains.ProjectType;

import java.time.LocalDateTime;
import java.util.List;

public class ProjectFactory implements Factory<ProjectDomain, ProjectJpaEntity> {

    private Long id;
    private String url;
    private String defaultBranch;
    private ProjectType projectType;
    private String name;
    private String localPath;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime indexedAt;
    private List<DocumentData> documents;

    public ProjectFactory withId(Long id) {
        this.id = id;
        return this;
    }

    public ProjectFactory withUrl(String url) {
        this.url = url;
        return this;
    }

    public ProjectFactory withDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
        return this;
    }

    public ProjectFactory withType(ProjectType projectType) {
        this.projectType = projectType;
        return this;
    }

    public ProjectFactory withName(String name) {
        this.name = name;
        return this;
    }

    public ProjectFactory withLocalPath(String localPath) {
        this.localPath = localPath;
        return this;
    }

    public ProjectFactory withCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public ProjectFactory withUpdatedAt(LocalDateTime modifiedAt) {
        this.updatedAt = modifiedAt;
        return this;
    }

    public ProjectFactory withIndexedAt(LocalDateTime indexedAt) {
        this.indexedAt = indexedAt;
        return this;
    }

    public ProjectFactory withDocuments(List<DocumentData> documents) {
        this.documents = documents;
        return this;
    }

    public static ProjectFactory from(ProjectData data) {
        ProjectFactory factory = new ProjectFactory()
                .withId(data.getId())
                .withUrl(data.getUrl())
                .withDefaultBranch(data.getDefaultBranch())
                .withType(data.getType())
                .withName(data.getName())
                .withLocalPath(data.getLocalPath())
                .withCreatedAt(data.getCreatedAt())
                .withUpdatedAt(data.getUpdatedAt())
                .withIndexedAt(data.getIndexedAt())
                .withDocuments(data.getDocuments());
        factory.validate();
        return factory;
    }

    @Override
    public ProjectDomain createDomain() {
        validate();
        ProjectDomain domain = new ProjectDomain();
        setCommonFields(domain);
        return domain;
    }

    @Override
    public ProjectJpaEntity createEntity() {
        validate();
        ProjectJpaEntity entity = new ProjectJpaEntity();
        setCommonFields(entity);
        return entity;
    }

    private void validate() {
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("URL is required");
        }
        if (projectType == null) {
            throw new IllegalStateException("Type is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalStateException("Name is required");
        }
        if (defaultBranch == null || defaultBranch.isBlank()) {
            throw new IllegalStateException("Default branch is required");
        }
    }

    private <T extends ProjectData> void setCommonFields(T data) {
        data.setId(id);
        data.setType(projectType);
        data.setUrl(url);
        data.setDefaultBranch(defaultBranch);
        data.setName(name);
        data.setCreatedAt(createdAt);
        data.setUpdatedAt(updatedAt);
        data.setIndexedAt(indexedAt);
        data.setDocuments(documents);
        data.setLocalPath(localPath);
    }
}