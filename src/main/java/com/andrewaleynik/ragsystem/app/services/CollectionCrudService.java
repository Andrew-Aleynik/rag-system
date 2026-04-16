package com.andrewaleynik.ragsystem.app.services;

import com.andrewaleynik.ragsystem.app.dto.project.request.collection.*;
import com.andrewaleynik.ragsystem.app.dto.project.response.CollectionListResponse;
import com.andrewaleynik.ragsystem.app.dto.project.response.CollectionResponse;
import com.andrewaleynik.ragsystem.app.dto.project.response.DocumentListResponse;
import com.andrewaleynik.ragsystem.app.dto.project.response.DocumentResponse;
import com.andrewaleynik.ragsystem.config.VectorStoreConfig;
import com.andrewaleynik.ragsystem.data.CollectionData;
import com.andrewaleynik.ragsystem.data.DocumentData;
import com.andrewaleynik.ragsystem.data.entities.CollectionJpaEntity;
import com.andrewaleynik.ragsystem.data.entities.DocumentJpaEntity;
import com.andrewaleynik.ragsystem.data.mappers.CollectionMapper;
import com.andrewaleynik.ragsystem.data.repositories.CollectionRepository;
import com.andrewaleynik.ragsystem.data.repositories.DocumentRepository;
import com.andrewaleynik.ragsystem.domains.CollectionDomain;
import com.andrewaleynik.ragsystem.factories.CollectionFactory;
import com.google.common.collect.Lists;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class CollectionCrudService {
    private final CollectionRepository collectionRepository;
    private final CollectionMapper collectionMapper;
    private final DocumentRepository documentRepository;
    private final VectorStoreConfig vectorStoreConfig;

    public CollectionResponse createCollection(CollectionCreateRequest request) {
        CollectionJpaEntity entity = new CollectionFactory()
                .withName(request.name())
                .createEntity();
        collectionRepository.save(entity);
        return createCollectionResponse(entity);
    }

    public CollectionListResponse retrieveCollections(CollectionRetrieveRequest request) {
        Iterable<CollectionJpaEntity> entities = collectionRepository.findAll();
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
        CollectionJpaEntity entity = collectionRepository.findById(request.id())
                .orElseThrow(() -> new EntityNotFoundException("Collection not found: " + request.id()));

        CollectionDomain domain = CollectionFactory.from(entity).createDomain();

        if (request.name() != null) {
            domain.setName(request.name());
        }

        collectionMapper.updateEntity(domain, entity);
        CollectionJpaEntity saved = collectionRepository.save(entity);

        return createCollectionResponse(saved);
    }

    public void deleteCollection(CollectionDeleteRequest request) {
        CollectionJpaEntity collection = collectionRepository.findById(request.id())
                .orElseThrow(() -> new EntityNotFoundException("Collection not found: " + request.id()));
        vectorStoreConfig.deleteVectorStore(collection);
        collectionRepository.deleteById(request.id());
    }

    @Transactional
    public void activateCollection(CollectionActivateRequest request) {
        CollectionJpaEntity collection = collectionRepository.findById(request.id())
                .orElseThrow(() -> new EntityNotFoundException("Collection not found: " + request.id()));
        collection.setActive(true);
        collectionRepository.save(collection);
    }

    @Transactional
    public void deactivateCollection(CollectionDeactivateRequest request) {
        CollectionJpaEntity collection = collectionRepository.findById(request.id())
                .orElseThrow(() -> new EntityNotFoundException("Collection not found: " + request.id()));
        collection.setActive(false);
        collectionRepository.save(collection);
    }

    @Transactional
    public DocumentListResponse addDocumentsToCollection(Long collectionId, List<Long> documentIds) {
        CollectionJpaEntity collection = collectionRepository.findById(collectionId).orElseThrow(() ->
                new EntityNotFoundException("Collection " + collectionId + "not found"));
        List<DocumentJpaEntity> documents = Lists.newArrayList(documentRepository.findAllById(documentIds));
        List<DocumentJpaEntity> addedDocuments = documents.stream()
                .filter(doc -> !collection.getDocuments().contains(doc))
                .toList();

        addedDocuments.forEach(collection::addDocument);

        if (!addedDocuments.isEmpty()) {
            collectionRepository.save(collection);
        }

        return new DocumentListResponse(
                addedDocuments.size(),
                addedDocuments.stream()
                        .map(this::createDocumentResponse)
                        .toList()
        );
    }

    @Transactional
    public DocumentListResponse removeDocumentsFromCollection(Long collectionId, List<Long> documentIds) {
        CollectionJpaEntity collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new EntityNotFoundException("Collection " + collectionId + " not found"));

        List<DocumentJpaEntity> documents = Lists.newArrayList(documentRepository.findAllById(documentIds));

        List<DocumentJpaEntity> removedDocuments = documents.stream()
                .filter(collection.getDocuments()::contains)
                .toList();

        removedDocuments.forEach(collection::removeDocument);

        if (!removedDocuments.isEmpty()) {
            collectionRepository.save(collection);
        }

        return new DocumentListResponse(
                removedDocuments.size(),
                removedDocuments.stream()
                        .map(this::createDocumentResponse)
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public DocumentListResponse getCollectionDocuments(Long collectionId) {
        CollectionJpaEntity collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new EntityNotFoundException("Collection " + collectionId + " not found"));

        List<DocumentData> documents = collection.getDocuments();

        return new DocumentListResponse(
                documents.size(),
                documents.stream()
                        .map(this::createDocumentResponse)
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public DocumentListResponse getCollectionDocuments(Long collectionId, int page, int size) {
        collectionRepository.findById(collectionId)
                .orElseThrow(() -> new EntityNotFoundException("Collection " + collectionId + " not found"));

        Pageable pageable = PageRequest.of(page, size);
        Page<DocumentJpaEntity> documentPage = documentRepository.findByCollectionId(collectionId, pageable);

        return new DocumentListResponse(
                (int) documentPage.getTotalElements(),
                documentPage.getContent().stream()
                        .map(this::createDocumentResponse)
                        .toList()
        );
    }

    private CollectionResponse createCollectionResponse(CollectionData collectionData) {
        return CollectionResponse.builder()
                .id(collectionData.getId())
                .createdAt(collectionData.getCreatedAt())
                .updatedAt(collectionData.getUpdatedAt())
                .name(collectionData.getName())
                .indexedAt(collectionData.getIndexedAt())
                .active(collectionData.getActive())
                .build();
    }

    private DocumentResponse createDocumentResponse(DocumentData documentData) {
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
}
