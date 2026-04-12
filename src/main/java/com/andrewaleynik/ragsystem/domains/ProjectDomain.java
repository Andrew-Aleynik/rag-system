package com.andrewaleynik.ragsystem.domains;

import com.andrewaleynik.ragsystem.data.DocumentData;
import com.andrewaleynik.ragsystem.data.ProjectData;
import lombok.Data;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProjectDomain implements ProjectData {
    private Long id;
    private String url;
    private String defaultBranch;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Path localPath;
    private String name;
    private ProjectType type;
    private LocalDateTime syncedAt;
    private LocalDateTime indexedAt;
    private List<DocumentData> documents = new ArrayList<>();

    private boolean connected = false;

    public Path getLocalPathAsPath() {
        return localPath;
    }

    @Override
    public void setLocalPath(String localPath) {
        this.localPath = localPath != null ? Paths.get(localPath) : null;
    }

    @Override
    public String getLocalPath() {
        return localPath != null ? localPath.toString() : null;
    }

    public List<DocumentData> getDocuments() {
        return new ArrayList<>(documents);
    }

    @Override
    public void setDocuments(List<DocumentData> documents) {
        this.documents.clear();
        if (documents != null) {
            documents.forEach(this::addDocument);
        }
    }

    @Override
    public void addDocument(DocumentData document) {
        this.documents.add(document);
    }

    @Override
    public void removeDocument(DocumentData document) {
        this.documents.removeIf(d -> d.getId().equals(document.getId()));
    }
}