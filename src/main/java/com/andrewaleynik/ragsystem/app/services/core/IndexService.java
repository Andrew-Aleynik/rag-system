package com.andrewaleynik.ragsystem.app.services.core;

import com.andrewaleynik.ragsystem.chunkers.Chunker;
import com.andrewaleynik.ragsystem.config.ChunkerConfig;
import com.andrewaleynik.ragsystem.config.VectorStoreConfig;
import com.andrewaleynik.ragsystem.data.DocumentContainer;
import com.andrewaleynik.ragsystem.data.Named;
import com.andrewaleynik.ragsystem.data.entities.ChunkJpaEntity;
import com.andrewaleynik.ragsystem.data.entities.DocumentJpaEntity;
import com.andrewaleynik.ragsystem.data.repositories.ChunkRepository;
import com.andrewaleynik.ragsystem.data.repositories.DocumentRepository;
import com.andrewaleynik.ragsystem.domains.ChunkDomain;
import com.andrewaleynik.ragsystem.exceptions.ChunkingException;
import com.andrewaleynik.ragsystem.factories.ChunkFactory;
import com.andrewaleynik.ragsystem.factories.DocumentFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IndexService {
    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;
    private final ChunkerConfig chunkerConfig;
    private final VectorStoreConfig vectorStoreConfig;

    public <T extends Named & DocumentContainer> void indexNamedDocumentContainer(T namedDocumentContainer) {
        VectorStore projectStore = vectorStoreConfig.getOrCreateVectorStore(namedDocumentContainer);
        List<ChunkJpaEntity> toSave = new ArrayList<>();
        List<ChunkJpaEntity> toRemove = new ArrayList<>();
        namedDocumentContainer.getDocuments().stream()
                .map(documentData -> DocumentFactory.from(documentData).createDomain())
                .forEach(document -> {
                    Chunker chunker = chunkerConfig.getChunkerForExtension(document.getFileExtension());
                    List<ChunkDomain> chunks;
                    try {
                        chunks = chunker.chunkDocument(document);
                    } catch (ChunkingException | IOException e) {
                        throw new RuntimeException(e);
                    }
                    List<ChunkJpaEntity> entities = chunkRepository.findAllByDocumentId(document.getId());
                    for (ChunkDomain chunk : chunks) {
                        Optional<ChunkJpaEntity> optionalEntity = entities.stream()
                                .filter(entity -> chunk.getIndex().equals(entity.getIndex())
                                        && chunk.getHash().equals(entity.getHash()))
                                .findFirst();
                        if (optionalEntity.isEmpty()) {
                            toSave.add(ChunkFactory.from(chunk)
                                    .withCreatedAt(LocalDateTime.now())
                                    .createEntity());
                        } else {
                            entities.remove(optionalEntity.get());
                        }
                    }
                    toRemove.addAll(entities);

                    document.setIndexedAt(LocalDateTime.now());
                    DocumentJpaEntity documentEntity = DocumentFactory.from(document).createEntity();
                    documentRepository.save(documentEntity);
                });

        List<Document> documentChunks = toSave.stream()
                .map(chunk -> new Document(chunk.getContent()))
                .toList();
        projectStore.add(documentChunks);
        for (int i = 0; i < documentChunks.size(); i++) {
            toSave.get(i).setVectorId(documentChunks.get(i).getId());
        }
        chunkRepository.saveAll(toSave);

        projectStore.delete(toRemove.stream().map(ChunkJpaEntity::getVectorId).toList());
        chunkRepository.deleteAllById(toRemove.stream().map(ChunkJpaEntity::getId).toList());
    }
}
