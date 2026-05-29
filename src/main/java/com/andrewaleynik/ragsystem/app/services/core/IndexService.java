package com.andrewaleynik.ragsystem.app.services.core;

import com.andrewaleynik.ragsystem.app.dto.response.ChunkDifference;
import com.andrewaleynik.ragsystem.chunkers.Chunker;
import com.andrewaleynik.ragsystem.config.ChunkerConfig;
import com.andrewaleynik.ragsystem.config.ExVectorStore;
import com.andrewaleynik.ragsystem.config.VectorStoreConfig;
import com.andrewaleynik.ragsystem.data.DocumentContainer;
import com.andrewaleynik.ragsystem.data.entities.Chunk;
import com.andrewaleynik.ragsystem.data.entities.Collection;
import com.andrewaleynik.ragsystem.data.entities.Document;
import com.andrewaleynik.ragsystem.data.entities.Project;
import com.andrewaleynik.ragsystem.data.repositories.ChunkRepository;
import com.andrewaleynik.ragsystem.data.repositories.CollectionRepository;
import com.andrewaleynik.ragsystem.data.repositories.DocumentRepository;
import com.andrewaleynik.ragsystem.data.repositories.ProjectRepository;
import com.andrewaleynik.ragsystem.exceptions.ChunkingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexService {
    private final ProjectRepository projectRepository;
    private final CollectionRepository collectionRepository;
    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;
    private final ChunkerConfig chunkerConfig;
    private final VectorStoreConfig vectorStoreConfig;
    private final VectorStoreHelper vectorStoreHelper;

    public void indexNamedDocumentContainer(DocumentContainer namedDocumentContainerEntity) {
        List<Chunk> toSave = new ArrayList<>();
        List<Chunk> toRemove = new ArrayList<>();

        Map<List<DocumentContainer>, List<Document>> containersDocuments =
                namedDocumentContainerEntity.getDocuments().stream()
                        .flatMap(document -> {
                            List<DocumentContainer> containers = findTargetNamedDocumentContainersByDocument(document);
                            return containers.stream()
                                    .map(container -> new AbstractMap.SimpleEntry<>(containers, document));
                        })
                        .collect(Collectors.groupingBy(
                                Map.Entry::getKey,
                                Collectors.mapping(Map.Entry::getValue,
                                        Collectors.collectingAndThen(
                                                Collectors.toList(),
                                                list -> list.stream().distinct().toList()
                                        ))
                        ));

        for (Map.Entry<List<DocumentContainer>, List<Document>> entry : containersDocuments.entrySet()) {
            List<DocumentContainer> containers = entry.getKey();
            List<Document> documents = entry.getValue();

            // 1. Чанкировать документы
            List<Chunk> newChunks = documents.stream()
                    .flatMap(doc -> chunkDocument(doc).stream())
                    .peek(chunk -> chunk.setCreatedAt(LocalDateTime.now()))
                    .toList();

            ChunkDifference diff = computeChunkDiff(documents, newChunks);
            toSave.addAll(diff.newChunks());
            toRemove.addAll(diff.deletedChunks());

            // 4. Для каждого контейнера: обновить вектора в Qdrant
            for (DocumentContainer container : containers) {
                ExVectorStore vectorStore = getOrCreateVectorStore(container);

                // 4a. Удалить старые вектора
                deleteVectorsFromVectorStore(vectorStore, diff.deletedChunks());

                // 4b. Добавить новые вектора
                indexChunksInVectorStore(vectorStore, diff.newChunks());
            }
            documents.stream()
                    .peek(document -> document.setIndexedAt(LocalDateTime.now()))
                    .forEach(documentRepository::save);
        }

        // 6. Обновить чанки в PostgreSQL
        if (!toRemove.isEmpty()) {
            chunkRepository.deleteAll(toRemove);
        }
        if (!toSave.isEmpty()) {
            chunkRepository.saveAll(toSave);
        }
        log.info("Indexing for {} success", namedDocumentContainerEntity.getName());
    }

    // Вычисление diff чанков
    private ChunkDifference computeChunkDiff(List<Document> documents, List<Chunk> newChunks) {
        Map<Long, List<Chunk>> newChunksByDoc = newChunks.stream()
                .collect(Collectors.groupingBy(Chunk::getDocumentId));

        List<Chunk> toCreate = new ArrayList<>();
        List<Chunk> toDelete = new ArrayList<>();
        List<Chunk> unchanged = new ArrayList<>();

        for (Document document : documents) {
            List<Chunk> existingChunks = chunkRepository.findAllByDocumentId(document.getId());
            List<Chunk> docNewChunks = newChunksByDoc.getOrDefault(document.getId(), List.of());

            Set<String> newSignatures = docNewChunks.stream()
                    .map(this::getChunkSignature)
                    .collect(Collectors.toSet());

            Set<String> existingSignatures = existingChunks.stream()
                    .map(this::getChunkSignature)
                    .collect(Collectors.toSet());

            // Новые чанки
            for (Chunk newChunk : docNewChunks) {
                if (!existingSignatures.contains(getChunkSignature(newChunk))) {
                    toCreate.add(newChunk);
                } else {
                    Chunk existing = existingChunks.stream()
                            .filter(ch -> getChunkSignature(ch).equals(getChunkSignature(newChunk)))
                            .findFirst()
                            .orElseThrow();
                    unchanged.add(existing);
                }
            }

            // Старые чанки
            for (Chunk existingChunk : existingChunks) {
                if (!newSignatures.contains(getChunkSignature(existingChunk))) {
                    toDelete.add(existingChunk);
                }
            }
        }

        return new ChunkDifference(toCreate, toDelete, unchanged);
    }

    private String getChunkSignature(Chunk chunk) {
        return chunk.getIndex() + "|" + chunk.getHash() + "|" + chunk.getEmbeddingModel();
    }

    // Удаление векторов из Qdrant
    private void deleteVectorsFromVectorStore(ExVectorStore vectorStore, List<Chunk> chunks) {
        if (chunks.isEmpty()) return;
        List<String> vectorIds = chunks.stream()
                .map(Chunk::getVectorId)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .toList();
        vectorStoreHelper.deleteVectorsFromVectorStore(vectorStore, vectorIds);
    }

    // Индексация чанков в Qdrant
    private void indexChunksInVectorStore(ExVectorStore vectorStore, List<Chunk> chunks) {
        if (chunks.isEmpty()) return;

        List<org.springframework.ai.document.Document> documentChunks = chunks.stream()
                .map(chunk -> new org.springframework.ai.document.Document(chunk.getContent()))
                .toList();
        vectorStore.vectorStore().add(documentChunks);
        EmbeddingResponse response = vectorStoreConfig.getEmbeddingModel().embedForResponse(List.of("Test"));
        String embeddingModelName = response.getMetadata().getModel();
        for (int i = 0; i < documentChunks.size(); i++) {
            Chunk chunk = chunks.get(i);
            org.springframework.ai.document.Document document = documentChunks.get(i);
            chunk.setVectorId(document.getId());
            chunk.setSizeTokens(countTokens(chunk.getContent()));
            chunk.setEmbeddingModel(embeddingModelName);
        }
    }

    private List<DocumentContainer> findTargetNamedDocumentContainersByDocument(Document document) {
        Project project = projectRepository.findByDocumentsContains(document);
        List<Collection> collections = collectionRepository.findAllByDocumentsContains(document);

        List<DocumentContainer> targetContainers = new ArrayList<>();
        targetContainers.add(project);
        targetContainers.addAll(collections.stream().map(DocumentContainer.class::cast).toList());
        return targetContainers;
    }

    private List<Chunk> chunkDocument(Document document) {
        Chunker chunker = chunkerConfig.getChunkerForExtension(document.getFileExtension());
        List<Chunk> chunks;
        try {
            chunks = chunker.chunkDocument(document);
        } catch (ChunkingException | IOException | StackOverflowError e) {
            log.warn("Error {}, during chunking with chunker: {}, try use default chunker", e, chunker);
            chunks = fallbackChunkDocument(document);
        }
        return chunks;
    }

    private List<Chunk> fallbackChunkDocument(Document document) {
        Chunker chunker = chunkerConfig.defaultChunker();
        List<Chunk> chunks = new ArrayList<>();
        try {
            chunks = chunker.chunkDocument(document);
        } catch (ChunkingException | IOException | StackOverflowError e1) {
            throw new RuntimeException(e1);
        }
        return chunks;
    }

    //TODO make accurate tokens counting
    public static int countTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.length() / 4;
    }

    private ExVectorStore getOrCreateVectorStore(DocumentContainer namedDocumentContainerEntity) {
        if (namedDocumentContainerEntity instanceof Project project) {
            return vectorStoreConfig.getOrCreateVectorStore(project);
        } else if (namedDocumentContainerEntity instanceof Collection collection) {
            return vectorStoreConfig.getOrCreateVectorStore(collection);
        } else {
            throw new IllegalArgumentException("Wrong type of namedDocumentContainerEntity: "
                    + namedDocumentContainerEntity.getClass());
        }
    }
}
