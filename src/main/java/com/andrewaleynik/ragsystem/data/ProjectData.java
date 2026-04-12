package com.andrewaleynik.ragsystem.data;

import com.andrewaleynik.ragsystem.domains.ProjectType;

import java.time.LocalDateTime;
import java.util.List;

public interface ProjectData {
    void setId(Long id);

    Long getId();

    void setType(ProjectType projectType);

    ProjectType getType();

    void setUrl(String url);

    String getUrl();

    void setLocalPath(String localPath);

    String getLocalPath();

    void setDefaultBranch(String defaultBranch);

    String getDefaultBranch();

    void setName(String name);

    String getName();

    void setCreatedAt(LocalDateTime createdAt);

    LocalDateTime getCreatedAt();

    void setUpdatedAt(LocalDateTime updatedAt);

    LocalDateTime getUpdatedAt();

    void setSyncedAt(LocalDateTime syncedAt);

    LocalDateTime getSyncedAt();

    void setIndexedAt(LocalDateTime indexedAt);

    LocalDateTime getIndexedAt();

    void setDocuments(List<DocumentData> documents);

    void addDocument(DocumentData document);

    void removeDocument(DocumentData document);

    List<DocumentData> getDocuments();
}
