package com.andrewaleynik.ragsystem.data.entities;

import com.andrewaleynik.ragsystem.data.CollectionData;
import com.andrewaleynik.ragsystem.data.DocumentData;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "Collection")
@Table(name = "collections", indexes = {
        @Index(name = "idx_collection_active", columnList = "active")
})
@Data
@ToString(exclude = "documents")
public class CollectionJpaEntity implements CollectionData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime indexedAt;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private Boolean active;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "collection_documents",
            joinColumns = @JoinColumn(name = "collection_id"),
            inverseJoinColumns = @JoinColumn(name = "document_id")
    )
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