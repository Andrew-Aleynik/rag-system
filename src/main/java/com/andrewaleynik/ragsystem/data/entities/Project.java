package com.andrewaleynik.ragsystem.data.entities;

import com.andrewaleynik.ragsystem.data.DocumentContainer;
import com.andrewaleynik.ragsystem.domains.ProjectType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "Project")
@Table(name = "projects", indexes = {
        @Index(name = "idx_project_url", columnList = "url"),
        @Index(name = "idx_project_active", columnList = "active")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project implements DocumentContainer {
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

    @Column(length = 1000, nullable = false, unique = true)
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

    @Builder.Default
    @OneToMany(mappedBy = "projectId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Document> documents = new ArrayList<>();

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

    public List<Document> getDocuments() {
        return new ArrayList<>(documents);
    }

    public void setDocuments(List<Document> documents) {
        this.documents.clear();
        if (documents != null) {
            documents.forEach(this::addDocument);
        }
    }

    public void addDocument(Document document) {
        this.documents.add(document);
    }

    public void removeDocument(Document document) {
        this.documents.removeIf(d -> d.getId().equals(document.getId()));
    }
}
