package com.andrewaleynik.ragsystem.app.services.rag;

import com.andrewaleynik.ragsystem.app.dto.project.request.RetrieveRequest;
import com.andrewaleynik.ragsystem.app.dto.project.response.RetrieveResponse;
import com.andrewaleynik.ragsystem.config.VectorStoreConfig;
import com.andrewaleynik.ragsystem.data.ChunkData;
import com.andrewaleynik.ragsystem.data.entities.ChunkJpaEntity;
import com.andrewaleynik.ragsystem.data.entities.CollectionJpaEntity;
import com.andrewaleynik.ragsystem.data.entities.ProjectJpaEntity;
import com.andrewaleynik.ragsystem.data.repositories.ChunkRepository;
import com.andrewaleynik.ragsystem.data.repositories.CollectionRepository;
import com.andrewaleynik.ragsystem.data.repositories.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetrieveService {
    private final CollectionRepository collectionRepository;
    private final ProjectRepository projectRepository;
    private final VectorStoreConfig vectorStoreConfig;
    private final ChunkRepository chunkRepository;

    @Value("${retrieve.top_k:5}")
    private int topK;

    @Value("${retrieve.similarity_threshold:0.7}")
    private double similarityThreshold;

    @Value("${retrieve.max_results:10}")
    private int maxResults;

    @Value("${retrieve.context_chunks_before:2}")
    private int contextChunksBefore;

    @Value("${retrieve.context_chunks_after:2}")
    private int contextChunksAfter;

    public RetrieveResponse retrieveChunks(RetrieveRequest request) {
        if (request == null || request.query() == null || request.query().isBlank()) {
            return new RetrieveResponse(Collections.emptyList());
        }

        List<ProjectJpaEntity> activeProjects = projectRepository.getAllByActive(true);
        List<CollectionJpaEntity> activeCollections = collectionRepository.getAllByActive(true);

        List<Document> allRetrievedDocuments = new ArrayList<>();

        for (ProjectJpaEntity project : activeProjects) {
            VectorStore vectorStore = vectorStoreConfig.getOrCreateVectorStore(project);
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(request.query())
                    .topK(topK)
                    .similarityThreshold(similarityThreshold)
                    .build();
            allRetrievedDocuments.addAll(vectorStore.similaritySearch(searchRequest));
        }

        for (CollectionJpaEntity collection : activeCollections) {
            VectorStore vectorStore = vectorStoreConfig.getOrCreateVectorStore(collection);
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(request.query())
                    .topK(topK)
                    .similarityThreshold(similarityThreshold)
                    .build();
            allRetrievedDocuments.addAll(vectorStore.similaritySearch(searchRequest));
        }

        List<String> mainVectorIds = allRetrievedDocuments.stream()
                .sorted(Comparator.comparing(Document::getScore, Comparator.reverseOrder()))
                .limit(maxResults)
                .map(Document::getId)
                .distinct()
                .toList();

        if (mainVectorIds.isEmpty()) {
            log.info("No relevant chunks found for query: '{}'", request.query());
            return new RetrieveResponse(Collections.emptyList());
        }

        List<ChunkJpaEntity> mainChunks = chunkRepository.findAllByVectorIdIn(mainVectorIds);

        Set<ChunkJpaEntity> allChunks = new LinkedHashSet<>(mainChunks);

        for (ChunkJpaEntity chunk : mainChunks) {
            List<ChunkJpaEntity> structuralChunks = chunkRepository.findByDocumentIdAndStructural(
                    chunk.getDocumentId(),
                    true
            );
            allChunks.addAll(structuralChunks);

            List<ChunkJpaEntity> neighborChunks = chunkRepository.findByDocumentIdAndStructuralAndIndexBetween(
                    chunk.getDocumentId(),
                    false,
                    chunk.getIndex() - contextChunksBefore,
                    chunk.getIndex() + contextChunksAfter
            );

            List<ChunkJpaEntity> filteredNeighbors = neighborChunks.stream()
                    .filter(c -> !c.getStructural())
                    .filter(c -> !allChunks.contains(c))
                    .toList();

            allChunks.addAll(filteredNeighbors);
        }

        List<ChunkData> sortedChunks = allChunks.stream()
                .sorted(Comparator.comparing(ChunkJpaEntity::getDocumentId).thenComparing(ChunkJpaEntity::getIndex))
                .map(ChunkData.class::cast)
                .toList();

        log.info("Retrieved {} main chunks, expanded to {} total chunks (structural + context)", mainChunks.size(),
                sortedChunks.size());

        return new RetrieveResponse(sortedChunks);
    }
}