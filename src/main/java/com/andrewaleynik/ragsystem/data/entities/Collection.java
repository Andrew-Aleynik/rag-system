package com.andrewaleynik.ragsystem.data.entities;

import com.andrewaleynik.ragsystem.data.DocumentContainer;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "Collection")
@Table(name = "collections", indexes = {
        @Index(name = "idx_collection_active", columnList = "active")
})
@Data
@ToString(exclude = "documents")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Collection implements DocumentContainer {
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

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "collection_documents",
            joinColumns = @JoinColumn(name = "collection_id"),
            inverseJoinColumns = @JoinColumn(name = "document_id")
    )
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

    @Override
    public List<Document> getDocuments() {
        return new ArrayList<>(documents);
    }

    @Override
    public void setDocuments(List<Document> documents) {
        this.documents.clear();
        if (documents != null) {
            documents.forEach(this::addDocument);
        }
    }

    @Override
    public void addDocument(Document document) {
        this.documents.add(document);
    }

    @Override
    public void removeDocument(Document document) {
        this.documents.removeIf(d -> d.getId().equals(document.getId()));
    }
}