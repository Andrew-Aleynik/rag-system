package com.andrewaleynik.ragsystem.config;

import com.andrewaleynik.ragsystem.data.CollectionData;
import com.andrewaleynik.ragsystem.data.ProjectData;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class VectorStoreConfig {

    private final EmbeddingModel embeddingModel;

    @Value("${qdrant.host:localhost}")
    private String qdrantHost;

    @Value("${qdrant.port:6334}")
    private int qdrantPort;

    @Value("${qdrant.use-tls:false}")
    private boolean useTls;

    @Value("${qdrant.api-key:}")
    private String apiKey;

    private final Map<String, VectorStore> vectorStores = new ConcurrentHashMap<>();

    @Bean
    public QdrantClient qdrantClient() {
        log.info("Creating QdrantClient for host: {}, port: {}, tls: {}", qdrantHost, qdrantPort, useTls);

        var builder = QdrantGrpcClient.newBuilder(qdrantHost, qdrantPort, useTls);

        if (apiKey != null && !apiKey.isEmpty()) {
            builder.withApiKey(apiKey);
        }

        return new QdrantClient(builder.build());
    }

    public VectorStore getOrCreateVectorStore(ProjectData projectData) {
        String collectionName = "project_" + projectData.getId();

        return vectorStores.computeIfAbsent(collectionName, id -> {
            log.info("Creating new VectorStore for project: {} (store: {})",
                    projectData.getName(), collectionName);

            return QdrantVectorStore.builder(qdrantClient(), embeddingModel)
                    .collectionName(collectionName)
                    .initializeSchema(true)
                    .build();
        });
    }

    public VectorStore getOrCreateVectorStore(CollectionData collectionData) {
        String collectionName = "collection_" + collectionData.getId();

        return vectorStores.computeIfAbsent(collectionName, id -> {
            log.info("Creating new VectorStore for collection: {} (store: {})",
                    collectionData.getName(), collectionName);

            return QdrantVectorStore.builder(qdrantClient(), embeddingModel)
                    .collectionName(collectionName)
                    .initializeSchema(true)
                    .build();
        });
    }

    @Bean
    @Lazy
    public VectorStore defaultVectorStore() {
        log.info("Creating default VectorStore with collection: default_collection");

        return QdrantVectorStore.builder(qdrantClient(), embeddingModel)
                .collectionName("default_collection")
                .initializeSchema(true)
                .build();
    }
}