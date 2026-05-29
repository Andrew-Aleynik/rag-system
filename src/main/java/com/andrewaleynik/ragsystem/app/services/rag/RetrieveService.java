package com.andrewaleynik.ragsystem.app.services.rag;

import com.andrewaleynik.ragsystem.app.dto.request.RetrieveRequest;
import com.andrewaleynik.ragsystem.app.dto.response.RetrieveResponse;
import com.andrewaleynik.ragsystem.config.ExVectorStore;
import com.andrewaleynik.ragsystem.config.VectorStoreConfig;
import com.andrewaleynik.ragsystem.data.entities.Chunk;
import com.andrewaleynik.ragsystem.data.entities.Collection;
import com.andrewaleynik.ragsystem.data.entities.Project;
import com.andrewaleynik.ragsystem.data.repositories.ChunkRepository;
import com.andrewaleynik.ragsystem.data.repositories.CollectionRepository;
import com.andrewaleynik.ragsystem.data.repositories.DocumentRepository;
import com.andrewaleynik.ragsystem.data.repositories.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetrieveService {
    private final CollectionRepository collectionRepository;
    private final ProjectRepository projectRepository;
    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;
    private final VectorStoreConfig vectorStoreConfig;

    @Value("${services.retrieve.top_k:5}")
    private int topK;

    @Value("${services.retrieve.similarity_threshold:0.7}")
    private double similarityThreshold;

    @Value("${services.retrieve.max_results:10}")
    private int maxResults;

    @Value("${services.retrieve.context_chunks_before:2}")
    private int contextChunksBefore;

    @Value("${services.retrieve.context_chunks_after:2}")
    private int contextChunksAfter;

    @Transactional
    public RetrieveResponse retrieveChunks(RetrieveRequest request) {
        if (request == null || request.query() == null || request.query().isBlank()) {
            return new RetrieveResponse(Collections.emptyMap());
        }

        List<Project> activeProjects = projectRepository.getAllByActive(true);
        List<Collection> activeCollections = collectionRepository.getAllByActive(true);

        List<Document> allRetrievedDocuments = new ArrayList<>();

        for (Project project : activeProjects) {
            ExVectorStore vectorStore = vectorStoreConfig.getOrCreateVectorStore(project);
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(request.query())
                    .topK(topK)
                    .similarityThreshold(similarityThreshold)
                    .build();
            allRetrievedDocuments.addAll(vectorStore.vectorStore().similaritySearch(searchRequest));
        }

        for (Collection collection : activeCollections) {
            ExVectorStore vectorStore = vectorStoreConfig.getOrCreateVectorStore(collection);
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(request.query())
                    .topK(topK)
                    .similarityThreshold(similarityThreshold)
                    .build();
            allRetrievedDocuments.addAll(vectorStore.vectorStore().similaritySearch(searchRequest));
        }

        List<String> mainVectorIds = allRetrievedDocuments.stream()
                .sorted(Comparator.comparing(Document::getScore, Comparator.reverseOrder()))
                .limit(maxResults)
                .map(Document::getId)
                .distinct()
                .toList();

        if (mainVectorIds.isEmpty()) {
            log.info("No relevant chunks found for query: '{}'", request.query());
            return new RetrieveResponse(Collections.emptyMap());
        }

        List<Chunk> mainChunks = chunkRepository.findAllByVectorIdIn(mainVectorIds);

        Set<Chunk> allChunks = new LinkedHashSet<>(mainChunks);

        for (Chunk chunk : mainChunks) {
            List<Chunk> structuralChunks = chunkRepository.findByDocumentIdAndStructural(
                    chunk.getDocumentId(),
                    true
            );
            allChunks.addAll(structuralChunks);

            List<Chunk> neighborChunks = chunkRepository.findByDocumentIdAndStructuralAndIndexBetween(
                    chunk.getDocumentId(),
                    false,
                    chunk.getIndex() - contextChunksBefore,
                    chunk.getIndex() + contextChunksAfter
            );

            List<Chunk> filteredNeighbors = neighborChunks.stream()
                    .filter(c -> !c.getStructural())
                    .filter(c -> !allChunks.contains(c))
                    .toList();

            allChunks.addAll(filteredNeighbors);
        }

        Set<Long> documentIds = allChunks.stream()
                .map(Chunk::getDocumentId)
                .collect(Collectors.toSet());

        Map<Long, com.andrewaleynik.ragsystem.data.entities.Document> documentsById;
        if (documentIds.isEmpty()) {
            documentsById = Collections.emptyMap();
        } else {
            documentsById = StreamSupport.stream(documentRepository.findAllById(documentIds).spliterator(), false)
                    .filter(document -> {
                        if (request.fileExtensions() != null && !request.fileExtensions().isEmpty()) {
                            return request.fileExtensions().contains(document.getFileExtension());
                        }
                        return true;
                    })
                    .collect(Collectors.toMap(
                            com.andrewaleynik.ragsystem.data.entities.Document::getId,
                            Function.identity()
                    ));
        }

        Map<String, List<Chunk>> documentChunks = allChunks.stream()
                .sorted(Comparator.comparing(Chunk::getDocumentId).thenComparing(Chunk::getIndex))
                .collect(Collectors.groupingBy(
                        chunk -> documentsById.get(chunk.getDocumentId()).getLocalPath(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        log.info("Retrieved {} main chunks, expanded to {} total chunks (structural + context)", mainChunks.size(),
                allChunks.size());

        return new RetrieveResponse(documentChunks);
    }
}