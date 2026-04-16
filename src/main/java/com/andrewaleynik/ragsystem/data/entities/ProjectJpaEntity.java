package com.andrewaleynik.ragsystem.data.entities;

import com.andrewaleynik.ragsystem.data.DocumentData;
import com.andrewaleynik.ragsystem.data.ProjectData;
import com.andrewaleynik.ragsystem.domains.ProjectType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "Project")
@Table(name = "projects", indexes = {
        @Index(name = "idx_project_url", columnList = "url"),
        @Index(name = "idx_project_active", columnList = "active")
})
@Data
public class ProjectJpaEntity implements ProjectData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 500, nullable = false, updatable = false, unique = true)
    private String url;

    @Column(length = 100)
    private String defaultBranch;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(length = 1000, unique = true)
    private String localPath;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ProjectType type;

    @Column
    private LocalDateTime syncedAt;

    @Column
    private LocalDateTime indexedAt;

    @Column(nullable = false)
    private Boolean active;

    @OneToMany(mappedBy = "projectId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DocumentJpaEntity> documents = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (active == null) {
            active = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
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
        if (document instanceof DocumentJpaEntity documentJpaEntity) {
            this.documents.add(documentJpaEntity);
        } else {
            throw new IllegalArgumentException("Expected DocumentJpaEntity");
        }
    }

    @Override
    public void removeDocument(DocumentData document) {
        this.documents.removeIf(d -> d.getId().equals(document.getId()));
    }
}
