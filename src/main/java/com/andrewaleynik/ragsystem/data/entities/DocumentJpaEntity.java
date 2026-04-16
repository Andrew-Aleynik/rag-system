package com.andrewaleynik.ragsystem.data.entities;

import com.andrewaleynik.ragsystem.data.ChunkData;
import com.andrewaleynik.ragsystem.data.DocumentData;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "Document")
@Table(name = "documents", indexes = {
        @Index(name = "idx_document_project_id", columnList = "project_id"),
        @Index(name = "idx_document_local_path", columnList = "local_path"),
})
@Data
@ToString(exclude = {"chunks", "collections"})
public class DocumentJpaEntity implements DocumentData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(length = 1000, unique = true)
    private String localPath;

    @Column(nullable = false)
    private String fileName;

    @Column(length = 100, nullable = false)
    private String fileExtension;

    @Column(length = 64, nullable = false)
    private String fileHash;

    @Column
    private LocalDateTime indexedAt;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "chunk_id", updatable = false)
    private List<ChunkJpaEntity> chunks = new ArrayList<>();

    @ManyToMany(mappedBy = "documents", fetch = FetchType.LAZY)
    private List<CollectionJpaEntity> collections = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public void setChunks(List<ChunkData> chunks) {
        this.chunks.clear();
        if (chunks != null) {
            chunks.forEach(this::addChunk);
        }
    }

    @Override
    public void addChunk(ChunkData chunk) {
        if (chunk instanceof ChunkJpaEntity chunkJpaEntity) {
            chunks.add(chunkJpaEntity);
        } else {
            throw new IllegalArgumentException("Expected ChunkJpaEntity");
        }
    }

    @Override
    public List<ChunkData> getChunks() {
        return new ArrayList<>(chunks);
    }

    @Override
    public void removeChunk(ChunkData chunk) {
        chunks.removeIf(c -> c.getId().equals(chunk.getId()));
    }
}
