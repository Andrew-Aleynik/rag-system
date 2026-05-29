package com.andrewaleynik.ragsystem.config;

import com.andrewaleynik.ragsystem.data.DocumentContainer;
import com.andrewaleynik.ragsystem.data.entities.Collection;
import com.andrewaleynik.ragsystem.data.entities.Project;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections;
import lombok.Getter;
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

    @Getter
    private final EmbeddingModel embeddingModel;

    @Value("${spring.ai.vectorestore.qdrant.host:localhost}")
    private String qdrantHost;

    @Value("${spring.ai.vectorestore.qdrant.port:6334}")
    private int qdrantPort;

    @Value("${spring.ai.vectorestore.qdrant.use-tls:false}")
    private boolean useTls;

    @Value("${spring.ai.vectorestore.qdrant.api-key:}")
    private String apiKey;

    private final Map<String, ExVectorStore> vectorStores = new ConcurrentHashMap<>();

    @Bean
    public QdrantClient qdrantClient() {
        log.info("Creating QdrantClient for host: {}, port: {}, tls: {}", qdrantHost, qdrantPort, useTls);

        var builder = QdrantGrpcClient.newBuilder(qdrantHost, qdrantPort, useTls);

        if (apiKey != null && !apiKey.isEmpty()) {
            builder.withApiKey(apiKey);
        }

        return new QdrantClient(builder.build());
    }

    public ExVectorStore getOrCreateVectorStore(Project projectData) {
        String collectionName = "project_" + projectData.getId();
        return getOrCreateVectorStoreInternal(collectionName, projectData);
    }

    public ExVectorStore getOrCreateVectorStore(Collection collection) {
        String collectionName = "collection_" + collection.getId();
        return getOrCreateVectorStoreInternal(collectionName, collection);
    }

    private ExVectorStore getOrCreateVectorStoreInternal(String collectionName, DocumentContainer documentContainer) {
        return vectorStores.computeIfAbsent(collectionName, id -> {
            QdrantClient client = qdrantClient();

            try {
                boolean exists = client.collectionExistsAsync(collectionName).get();

                if (!exists) {
                    log.info("Creating new VectorStore for: {}, collection: {}", documentContainer.getName(), collectionName);
                    log.info("Embedding vector size: {}", embeddingModel.dimensions());

                    client.createCollectionAsync(collectionName,
                            Collections.VectorParams.newBuilder()
                                    .setSize(embeddingModel.dimensions())
                                    .setDistance(Collections.Distance.Cosine)
                                    .build()
                    ).get();

                    log.info("Collection {} created in Qdrant", collectionName);
                }
            } catch (Exception e) {
                log.error("Failed to create collection {} in Qdrant: {}", collectionName, e.getMessage());
                throw new RuntimeException("Cannot create Qdrant collection: " + collectionName, e);
            }

            VectorStore store = QdrantVectorStore.builder(client, embeddingModel)
                    .collectionName(collectionName)
                    .initializeSchema(false)
                    .build();

            log.info("VectorStore fetched successfully: {}", collectionName);
            return new ExVectorStore(collectionName, store);
        });
    }

    public void deleteVectorStore(Project project) {
        String collectionName = "project_" + project.getId();
        deleteCollection(collectionName);
    }

    public void deleteVectorStore(Collection collection) {
        String collectionName = "collection_" + collection.getId();
        deleteCollection(collectionName);
    }

    private void deleteCollection(String collectionName) {
        try {
            QdrantClient client = qdrantClient();

            if (Boolean.TRUE.equals(client.collectionExistsAsync(collectionName).get())) {
                client.deleteCollectionAsync(collectionName).get();
                vectorStores.remove(collectionName);
                log.info("Collection deleted: {}", collectionName);
            } else {
                log.debug("Collection does not exist, nothing to delete: {}", collectionName);
            }
        } catch (Exception e) {
            log.error("Failed to delete collection {}: {}", collectionName, e.getMessage());
        }
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