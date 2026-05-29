package com.andrewaleynik.ragsystem.data;

import com.andrewaleynik.ragsystem.data.entities.Document;

import java.util.List;

public interface DocumentContainer {
    Long getId();

    void setId(Long id);

    List<Document> getDocuments();

    void setDocuments(List<Document> documents);

    void addDocument(Document document);

    void removeDocument(Document document);

    String getName();

    void setName(String name);
}
