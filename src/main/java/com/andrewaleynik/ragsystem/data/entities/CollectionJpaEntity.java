package com.andrewaleynik.ragsystem.data.entities;

import com.andrewaleynik.ragsystem.data.CollectionData;
import com.andrewaleynik.ragsystem.data.DocumentData;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "Collection")
@Table(name = "collections")
@Data
public class CollectionJpaEntity implements CollectionData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column
    private LocalDateTime createdAt;
    @Column
    private LocalDateTime updatedAt;
    @Column
    private LocalDateTime indexedAt;
    @Column
    private String name;
    @Column
    private Boolean active;
    @ManyToMany(targetEntity = DocumentJpaEntity.class, fetch = FetchType.EAGER)
    @JoinTable(
            name = "collection_documents",
            joinColumns = @JoinColumn(name = "collection_id"),
            inverseJoinColumns = @JoinColumn(name = "document_id")
    )
    private List<DocumentData> documents;

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
