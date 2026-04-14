package com.andrewaleynik.ragsystem.app.services;

import com.andrewaleynik.ragsystem.app.dto.project.request.collection.CollectionDeleteRequest;
import com.andrewaleynik.ragsystem.app.dto.project.request.collection.CollectionRetrieveRequest;
import com.andrewaleynik.ragsystem.app.dto.project.request.collection.CollectionUpdateRequest;
import com.andrewaleynik.ragsystem.app.dto.project.request.project.ProjectCreateRequest;
import com.andrewaleynik.ragsystem.app.dto.project.response.CollectionListResponse;
import com.andrewaleynik.ragsystem.app.dto.project.response.CollectionResponse;
import com.andrewaleynik.ragsystem.data.CollectionData;
import com.andrewaleynik.ragsystem.data.entities.CollectionJpaEntity;
import com.andrewaleynik.ragsystem.data.mappers.CollectionMapper;
import com.andrewaleynik.ragsystem.data.repositories.CollectionRepository;
import com.andrewaleynik.ragsystem.domains.CollectionDomain;
import com.andrewaleynik.ragsystem.factories.CollectionFactory;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class CollectionCrudService {
    private final CollectionRepository collectionRepository;
    private final CollectionMapper collectionMapper;

    public CollectionResponse createCollection(ProjectCreateRequest request) {
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
        if (!collectionRepository.existsById(request.id())) {
            throw new EntityNotFoundException("Collection not found: " + request.id());
        }
        collectionRepository.deleteById(request.id());
    }

    private CollectionResponse createCollectionResponse(CollectionData collectionData) {
        return CollectionResponse.builder()
                .id(collectionData.getId())
                .createdAt(collectionData.getCreatedAt())
                .updatedAt(collectionData.getUpdatedAt())
                .name(collectionData.getName())
                .indexedAt(collectionData.getIndexedAt())
                .build();
    }
}
