package com.andrewaleynik.ragsystem.data;

import java.util.List;

public interface DocumentContainer {
    List<DocumentData> getDocuments();

    void setDocuments(List<DocumentData> documents);

    void addDocument(DocumentData document);

    void removeDocument(DocumentData document);
}
