package com.andrewaleynik.ragsystem.data;

import com.andrewaleynik.ragsystem.domains.ProjectType;

import java.time.LocalDateTime;

public interface ProjectData extends DocumentContainer, Named, Entity {
    void setType(ProjectType projectType);

    ProjectType getType();

    void setUrl(String url);

    String getUrl();

    void setLocalPath(String localPath);

    String getLocalPath();

    void setDefaultBranch(String defaultBranch);

    String getDefaultBranch();

    void setCreatedAt(LocalDateTime createdAt);

    LocalDateTime getCreatedAt();

    void setUpdatedAt(LocalDateTime updatedAt);

    LocalDateTime getUpdatedAt();

    void setSyncedAt(LocalDateTime syncedAt);

    LocalDateTime getSyncedAt();

    void setIndexedAt(LocalDateTime indexedAt);

    LocalDateTime getIndexedAt();

    void setActive(Boolean active);

    Boolean getActive();
}
