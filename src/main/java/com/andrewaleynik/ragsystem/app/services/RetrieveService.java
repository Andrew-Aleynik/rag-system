package com.andrewaleynik.ragsystem.app.services;

import com.andrewaleynik.ragsystem.app.dto.project.request.RetrieveRequest;
import com.andrewaleynik.ragsystem.config.VectorStoreConfig;
import com.andrewaleynik.ragsystem.data.repositories.ChunkRepository;
import com.andrewaleynik.ragsystem.domains.ChunkDomain;
import com.andrewaleynik.ragsystem.domains.ProjectDomain;
import com.andrewaleynik.ragsystem.factories.ChunkFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetrieveService {
    private final VectorStoreConfig vectorStoreConfig;
    private final ChunkRepository chunkRepository;
    @Value("${retrieve.top_k:5}")
    private int topK;
    @Value("${retrieve.similarity_threshold:0.7}")
    private double similarityThreshold;
    @Value("${retrieve.max_results:10}")
    private int maxResults;


    public List<ChunkDomain> retrieveChunks(RetrieveRequest request) {
        if (request == null || request.query() == null || request.query().isBlank()) {
            return Collections.emptyList();
        }

        List<Document> allRetrievedDocuments = new ArrayList<>();
        for (ProjectDomain project : request.projects()) {
            VectorStore vectorStore = vectorStoreConfig.getOrCreateVectorStore(project);
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(request.query())
                    .topK(topK)
                    .similarityThreshold(0.7)
                    .build();
            allRetrievedDocuments.addAll(vectorStore.similaritySearch(searchRequest));
        }

        List<String> vectorIds = allRetrievedDocuments.stream()
                .sorted(Comparator.comparing(Document::getScore, Comparator.reverseOrder()))
                .limit(maxResults)
                .map(Document::getId)
                .distinct()
                .toList();

        if (vectorIds.isEmpty()) {
            log.info("No relevant chunks found for query: '{}'", request.query());
            return new ArrayList<>();
        }

        return chunkRepository.findAllByVectorIdIn(vectorIds).stream()
                .map(chunkEntity -> ChunkFactory.from(chunkEntity).createDomain())
                .toList();
    }
}
