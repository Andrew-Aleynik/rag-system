package com.andrewaleynik.ragsystem.data.entities;

import com.andrewaleynik.ragsystem.data.DocumentData;
import com.andrewaleynik.ragsystem.data.ProjectData;
import com.andrewaleynik.ragsystem.domains.ProjectType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "Project")
@Table(name = "projects")
@Data
public class ProjectJpaEntity implements ProjectData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(length = 500, nullable = false, updatable = false)
    private String url;
    @Column(nullable = false)
    private String defaultBranch;
    @Column
    private LocalDateTime createdAt;
    @Column
    private LocalDateTime updatedAt;
    @Column(length = 1000)
    private String localPath;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private ProjectType type;
    @Column
    private LocalDateTime syncedAt;
    @Column
    private LocalDateTime indexedAt;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "project_id")
    private List<DocumentJpaEntity> documents = new ArrayList<>();

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
        if (document instanceof DocumentJpaEntity) {
            this.documents.add((DocumentJpaEntity) document);
        } else {
            throw new IllegalArgumentException("Expected DocumentJpaEntity");
        }
    }

    @Override
    public void removeDocument(DocumentData document) {
        this.documents.removeIf(d -> d.getId().equals(document.getId()));
    }
}
