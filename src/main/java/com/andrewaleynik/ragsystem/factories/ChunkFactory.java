package com.andrewaleynik.ragsystem.factories;

import com.andrewaleynik.ragsystem.data.ChunkData;
import com.andrewaleynik.ragsystem.data.entities.ChunkJpaEntity;
import com.andrewaleynik.ragsystem.domains.ChunkDomain;

import java.time.LocalDateTime;

public class ChunkFactory implements Factory<ChunkDomain, ChunkJpaEntity> {
    private Long id;
    private String vectorId;
    private Long documentId;
    private Boolean structural;
    private String content;
    private String hash;
    private Integer index;
    private Integer sizeBytes;
    private Integer sizeTokens;
    private LocalDateTime createdAt;
    private LocalDateTime indexedAt;

    public ChunkFactory withId(Long id) {
        this.id = id;
        return this;
    }

    public ChunkFactory withVectorId(String vectorId) {
        this.vectorId = vectorId;
        return this;
    }

    public ChunkFactory withDocumentId(Long documentId) {
        this.documentId = documentId;
        return this;
    }

    public ChunkFactory withStructural(Boolean structural) {
        this.structural = structural;
        return this;
    }

    public ChunkFactory withContent(String content) {
        this.content = content;
        return this;
    }

    public ChunkFactory withHash(String hash) {
        this.hash = hash;
        return this;
    }

    public ChunkFactory withIndex(Integer index) {
        this.index = index;
        return this;
    }

    public ChunkFactory withSizeBytes(Integer sizeBytes) {
        this.sizeBytes = sizeBytes;
        return this;
    }

    public ChunkFactory withSizeTokens(Integer sizeTokens) {
        this.sizeTokens = sizeTokens;
        return this;
    }

    public ChunkFactory withCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public ChunkFactory withIndexedAt(LocalDateTime indexedAt) {
        this.indexedAt = indexedAt;
        return this;
    }

    public static ChunkFactory from(ChunkData data) {
        return new ChunkFactory()
                .withId(data.getId())
                .withVectorId(data.getVectorId())
                .withDocumentId(data.getDocumentId())
                .withStructural(data.getStructural())
                .withContent(data.getContent())
                .withHash(data.getHash())
                .withIndex(data.getIndex())
                .withSizeBytes(data.getSizeBytes())
                .withSizeTokens(data.getSizeTokens())
                .withCreatedAt(data.getCreatedAt())
                .withIndexedAt(data.getIndexedAt());
    }

    @Override
    public ChunkDomain createDomain() {
        ChunkDomain domain = new ChunkDomain();
        setCommonFields(domain);
        return domain;
    }

    @Override
    public ChunkJpaEntity createEntity() {
        ChunkJpaEntity entity = new ChunkJpaEntity();
        setCommonFields(entity);
        return entity;
    }

    private <T extends ChunkData> void setCommonFields(T data) {
        data.setId(id);
        data.setVectorId(vectorId);
        data.setDocumentId(documentId);
        data.setStructural(structural);
        data.setContent(content);
        data.setHash(hash);
        data.setIndex(index);
        data.setSizeBytes(sizeBytes);
        data.setSizeTokens(sizeTokens);
        data.setCreatedAt(createdAt);
        data.setIndexedAt(indexedAt);
    }
}
