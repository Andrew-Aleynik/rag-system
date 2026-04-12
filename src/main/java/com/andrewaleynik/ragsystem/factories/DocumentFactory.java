package com.andrewaleynik.ragsystem.factories;

import com.andrewaleynik.ragsystem.data.ChunkData;
import com.andrewaleynik.ragsystem.data.DocumentData;
import com.andrewaleynik.ragsystem.data.entities.DocumentJpaEntity;
import com.andrewaleynik.ragsystem.domains.DocumentDomain;

import java.time.LocalDateTime;
import java.util.List;

public class DocumentFactory implements Factory<DocumentDomain, DocumentJpaEntity> {
    private Long id;
    private Long projectId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String localPath;
    private String fileName;
    private String fileExtension;
    private String fileHash;
    private LocalDateTime indexedAt;
    private List<ChunkData> chunks;

    public DocumentFactory withId(Long id) {
        this.id = id;
        return this;
    }

    public DocumentFactory withProjectId(Long projectId) {
        this.projectId = projectId;
        return this;
    }

    public DocumentFactory withLocalPath(String localPath) {
        this.localPath = localPath;
        return this;
    }

    public DocumentFactory withCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public DocumentFactory withUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public DocumentFactory withFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public DocumentFactory withFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
        return this;
    }

    public DocumentFactory withFileHash(String fileHash) {
        this.fileHash = fileHash;
        return this;
    }

    public DocumentFactory withIndexedAt(LocalDateTime indexedAt) {
        this.indexedAt = indexedAt;
        return this;
    }

    public DocumentFactory withChunks(List<ChunkData> chunks) {
        this.chunks = chunks;
        return this;
    }

    public static DocumentFactory from(DocumentData data) {
        return new DocumentFactory()
                .withId(data.getId())
                .withProjectId(data.getProjectId())
                .withCreatedAt(data.getCreatedAt())
                .withUpdatedAt(data.getUpdatedAt())
                .withLocalPath(data.getLocalPath())
                .withFileName(data.getFileName())
                .withFileExtension(data.getFileExtension())
                .withFileHash(data.getFileHash())
                .withIndexedAt(data.getIndexedAt())
                .withChunks(data.getChunks());
    }

    @Override
    public DocumentDomain createDomain() {
        DocumentDomain domain = new DocumentDomain();
        setCommonFields(domain);
        return domain;
    }

    @Override
    public DocumentJpaEntity createEntity() {
        DocumentJpaEntity entity = new DocumentJpaEntity();
        setCommonFields(entity);
        return entity;
    }

    private <T extends DocumentData> void setCommonFields(T data) {
        data.setId(this.id);
        data.setProjectId(this.projectId);
        data.setCreatedAt(this.createdAt);
        data.setUpdatedAt(this.updatedAt);
        data.setFileName(this.fileName);
        data.setFileExtension(this.fileExtension);
        data.setFileHash(this.fileHash);
        data.setIndexedAt(this.indexedAt);
        data.setChunks(this.chunks);
        data.setLocalPath(this.localPath);
    }
}
