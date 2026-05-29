package com.andrewaleynik.ragsystem.app.services;

import com.andrewaleynik.ragsystem.app.dto.request.collection.*;
import com.andrewaleynik.ragsystem.app.dto.response.CollectionListResponse;
import com.andrewaleynik.ragsystem.app.dto.response.CollectionResponse;
import com.andrewaleynik.ragsystem.app.dto.response.DocumentListResponse;
import com.andrewaleynik.ragsystem.app.dto.response.DocumentResponse;
import com.andrewaleynik.ragsystem.app.services.core.VectorStoreHelper;
import com.andrewaleynik.ragsystem.config.ExVectorStore;
import com.andrewaleynik.ragsystem.config.VectorStoreConfig;
import com.andrewaleynik.ragsystem.data.entities.Chunk;
import com.andrewaleynik.ragsystem.data.entities.Collection;
import com.andrewaleynik.ragsystem.data.entities.Document;
import com.andrewaleynik.ragsystem.data.entities.Project;
import com.andrewaleynik.ragsystem.data.repositories.ChunkRepository;
import com.andrewaleynik.ragsystem.data.repositories.CollectionRepository;
import com.andrewaleynik.ragsystem.data.repositories.DocumentRepository;
import com.andrewaleynik.ragsystem.data.repositories.ProjectRepository;
import com.google.common.collect.Lists;
import io.qdrant.client.grpc.Points;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class CollectionCrudService {
    private final ProjectRepository projectRepository;
    private final CollectionRepository collectionRepository;
    private final DocumentRepository documentRepository;

    private final ChunkRepository chunkRepository;
    private final VectorStoreConfig vectorStoreConfig;
    private final VectorStoreHelper vectorStoreHelper;

    @Transactional
    public CollectionResponse createCollection(CollectionCreateRequest request) {
        Collection collection = Collection.builder()
                .name(request.name())
                .build();
        collectionRepository.save(collection);
        return createCollectionResponse(collection);
    }

    @Transactional(readOnly = true)
    public CollectionListResponse retrieveCollections(CollectionRetrieveRequest request) {
        Iterable<Collection> entities = collectionRepository.findAllById(request.ids());
        List<CollectionResponse> collections = StreamSupport.stream(entities.spliterator(), false)
                .map(this::createCollectionResponse)
                .toList();
        return new CollectionListResponse(
                collections.size(),
                collections
        );
    }

    @Transactional(readOnly = true)
    public CollectionListResponse retrieveCollections() {
        Iterable<Collection> entities = collectionRepository.findAll();
        List<CollectionResponse> collections = StreamSupport.stream(entities.spliterator(), false)
                .map(this::createCollectionResponse)
                .toList();
        return new CollectionListResponse(
                collections.size(),
                collections
        );
    }

    @Transactional
    public CollectionResponse updateCollection(CollectionUpdateRequest request) {
        Collection collection = collectionRepository.findById(request.id())
                .orElseThrow(() -> new EntityNotFoundException("Collection not found: " + request.id()));

        if (request.name() != null) {
            collection.setName(request.name());
        }

        Collection saved = collectionRepository.save(collection);

        return createCollectionResponse(saved);
    }

    @Transactional
    public void deleteCollection(CollectionDeleteRequest request) {
        Collection collection = collectionRepository.findById(request.id())
                .orElseThrow(() -> new EntityNotFoundException("Collection not found: " + request.id()));
        vectorStoreConfig.deleteVectorStore(collection);
        collectionRepository.deleteById(request.id());
    }

    @Transactional
    public void activateCollection(CollectionActivateRequest request) {
        Collection collection = collectionRepository.findById(request.id())
                .orElseThrow(() -> new EntityNotFoundException("Collection not found: " + request.id()));
        collection.setActive(true);
        collectionRepository.save(collection);
    }

    @Transactional
    public void deactivateCollection(CollectionDeactivateRequest request) {
        Collection collection = collectionRepository.findById(request.id())
                .orElseThrow(() -> new EntityNotFoundException("Collection not found: " + request.id()));
        collection.setActive(false);
        collectionRepository.save(collection);
    }

    @Transactional
    public DocumentListResponse addDocumentsToCollection(Long collectionId, List<Long> documentIds) {
        Collection collection = collectionRepository.findById(collectionId).orElseThrow(() ->
                new EntityNotFoundException("Collection " + collectionId + " not found"));
        List<Document> documents = Lists.newArrayList(documentRepository.findAllById(documentIds));
        List<Document> addedDocuments = documents.stream()
                .filter(doc -> !collection.getDocuments().contains(doc))
                .toList();

        if (addedDocuments.isEmpty()) {
            return new DocumentListResponse(0, 0, List.of());
        }

        addedDocuments.forEach(collection::addDocument);
        collectionRepository.save(collection);
        copyVectorsToCollection(collection, addedDocuments);

        return new DocumentListResponse(
                addedDocuments.size(),
                addedDocuments.size(),
                addedDocuments.stream()
                        .map(this::createDocumentResponse)
                        .toList()
        );
    }

    @Transactional
    public DocumentListResponse removeDocumentsFromCollection(Long collectionId, List<Long> documentIds) {
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new EntityNotFoundException("Collection " + collectionId + " not found"));

        List<Document> documents = Lists.newArrayList(documentRepository.findAllById(documentIds));

        List<Document> removedDocuments = documents.stream()
                .filter(collection.getDocuments()::contains)
                .toList();

        if (removedDocuments.isEmpty()) {
            return new DocumentListResponse(0, 0, List.of());
        }

        removedDocuments.forEach(collection::removeDocument);
        collectionRepository.save(collection);
        deleteVectorsFromCollection(collection, removedDocuments);

        return new DocumentListResponse(
                removedDocuments.size(),
                removedDocuments.size(),
                removedDocuments.stream()
                        .map(this::createDocumentResponse)
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public DocumentListResponse getCollectionDocuments(Long collectionId) {
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new EntityNotFoundException("Collection " + collectionId + " not found"));

        List<Document> documents = collection.getDocuments();

        return new DocumentListResponse(
                documents.size(),
                documents.size(),
                documents.stream()
                        .map(this::createDocumentResponse)
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public DocumentListResponse getCollectionDocuments(Long collectionId, int page, int size) {
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new EntityNotFoundException("Collection " + collectionId + " not found"));

        Pageable pageable = PageRequest.of(page, size);
        Page<Document> documentPage = documentRepository.findByCollectionId(collectionId, pageable);

        return new DocumentListResponse(
                collection.getDocuments().size(),
                (int) documentPage.getTotalElements(),
                documentPage.getContent().stream()
                        .map(this::createDocumentResponse)
                        .toList()
        );
    }

    private CollectionResponse createCollectionResponse(Collection collectionData) {
        return CollectionResponse.builder()
                .id(collectionData.getId())
                .createdAt(collectionData.getCreatedAt())
                .updatedAt(collectionData.getUpdatedAt())
                .name(collectionData.getName())
                .indexedAt(collectionData.getIndexedAt())
                .active(collectionData.getActive())
                .documentIds(collectionData.getDocuments().stream().map(Document::getId).toList())
                .build();
    }

    private DocumentResponse createDocumentResponse(Document documentData) {
        return DocumentResponse.builder()
                .id(documentData.getId())
                .projectId(documentData.getProjectId())
                .createdAt(documentData.getCreatedAt())
                .updatedAt(documentData.getUpdatedAt())
                .indexedAt(documentData.getIndexedAt())
                .localPath(documentData.getLocalPath())
                .fileName(documentData.getFileName())
                .fileExtension(documentData.getFileExtension())
                .fileHash(documentData.getFileHash())
                .build();
    }

    private void copyVectorsToCollection(Collection collection, List<Document> documents) {
        ExVectorStore collectionVectorStore = vectorStoreConfig.getOrCreateVectorStore(collection);

        Map<Long, List<String>> chunksByProject = documents.stream()
                .collect(Collectors.groupingBy(
                        Document::getProjectId,
                        Collectors.flatMapping(
                                doc -> chunkRepository.findAllByDocumentId(doc.getId()).stream().map(Chunk::getVectorId),
                                Collectors.toList()
                        )
                ));

        chunksByProject.forEach((projectId, vectorIds) -> {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new EntityNotFoundException("Project not found: " + projectId));
            ExVectorStore projectVectorStore = vectorStoreConfig.getOrCreateVectorStore(project);
            List<Points.PointStruct> pointsToCopy = vectorStoreHelper.fetchVectorsFromVectorStore(projectVectorStore, vectorIds);
            vectorStoreHelper.writeVectorsToVectorStore(collectionVectorStore, pointsToCopy);
        });
    }

    private void deleteVectorsFromCollection(Collection collection, List<Document> documents) {
        ExVectorStore collectionVectorStore = vectorStoreConfig.getOrCreateVectorStore(collection);

        List<String> vectorIds = documents.stream()
                .flatMap(doc -> chunkRepository.findAllByDocumentId(doc.getId()).stream())
                .map(Chunk::getVectorId)
                .map(String::valueOf)
                .toList();

        if (vectorIds.isEmpty()) {
            return;
        }
        collectionVectorStore.vectorStore().delete(vectorIds);
    }
}
