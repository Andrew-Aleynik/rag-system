package com.andrewaleynik.ragsystem.config;

import org.springframework.ai.vectorstore.VectorStore;

public record ExVectorStore(String collectionName, VectorStore vectorStore) {
}
