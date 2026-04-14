package com.andrewaleynik.ragsystem.factories;

import com.andrewaleynik.ragsystem.data.CollectionData;
import com.andrewaleynik.ragsystem.data.DocumentData;
import com.andrewaleynik.ragsystem.data.entities.CollectionJpaEntity;
import com.andrewaleynik.ragsystem.domains.CollectionDomain;

import java.time.LocalDateTime;
import java.util.List;

public class CollectionFactory implements Factory<CollectionDomain, CollectionJpaEntity> {
    private Long id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime indexedAt;
    private String name;
    private Boolean active;
    private List<DocumentData> documents;

    public CollectionFactory withId(Long id) {
        this.id = id;
        return this;
    }

    public CollectionFactory withName(String name) {
        this.name = name;
        return this;
    }

    public CollectionFactory withCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public CollectionFactory withUpdatedAt(LocalDateTime modifiedAt) {
        this.updatedAt = modifiedAt;
        return this;
    }

    public CollectionFactory withIndexedAt(LocalDateTime indexedAt) {
        this.indexedAt = indexedAt;
        return this;
    }

    public CollectionFactory withDocuments(List<DocumentData> documents) {
        this.documents = documents;
        return this;
    }

    public CollectionFactory withActive(Boolean active) {
        this.active = active;
        return this;
    }

    public static CollectionFactory from(CollectionData data) {
        CollectionFactory factory = new CollectionFactory()
                .withId(data.getId())
                .withName(data.getName())
                .withCreatedAt(data.getCreatedAt())
                .withUpdatedAt(data.getUpdatedAt())
                .withIndexedAt(data.getIndexedAt())
                .withActive(data.getActive())
                .withDocuments(data.getDocuments());
        factory.validate();
        return factory;
    }

    @Override
    public CollectionDomain createDomain() {
        validate();
        CollectionDomain domain = new CollectionDomain();
        setCommonFields(domain);
        return domain;
    }

    @Override
    public CollectionJpaEntity createEntity() {
        validate();
        CollectionJpaEntity entity = new CollectionJpaEntity();
        setCommonFields(entity);
        return entity;
    }

    private void validate() {
        if (name == null || name.isBlank()) {
            throw new IllegalStateException("Name is required");
        }
    }

    private <T extends CollectionData> void setCommonFields(T data) {
        data.setId(id);
        data.setName(name);
        data.setCreatedAt(createdAt);
        data.setUpdatedAt(updatedAt);
        data.setIndexedAt(indexedAt);
        data.setActive(active);
        data.setDocuments(documents);
    }
}
