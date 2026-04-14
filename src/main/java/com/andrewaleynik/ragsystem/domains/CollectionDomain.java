package com.andrewaleynik.ragsystem.domains;

import com.andrewaleynik.ragsystem.data.CollectionData;
import com.andrewaleynik.ragsystem.data.DocumentData;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class CollectionDomain implements CollectionData {
    private Long id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime indexedAt;
    private String name;
    private Boolean active;
    private List<DocumentData> documents;

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
