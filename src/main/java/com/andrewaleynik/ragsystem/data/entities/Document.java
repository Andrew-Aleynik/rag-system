package com.andrewaleynik.ragsystem.data.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "Document")
@Table(name = "documents", indexes = {
        @Index(name = "idx_document_project_id", columnList = "project_id"),
        @Index(name = "idx_document_local_path", columnList = "local_path"),
})
@Data
@Builder
@ToString(exclude = {"chunks", "collections"})
@NoArgsConstructor
@AllArgsConstructor
public class Document {
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
    @Builder.Default
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "chunk_id", updatable = false)
    private List<Chunk> chunks = new ArrayList<>();

    @Builder.Default
    @ManyToMany(mappedBy = "documents", fetch = FetchType.LAZY)
    private List<Collection> collections = new ArrayList<>();

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

    public void setChunks(List<Chunk> chunks) {
        this.chunks.clear();
        if (chunks != null) {
            chunks.forEach(this::addChunk);
        }
    }

    public void addChunk(Chunk chunk) {
        chunks.add(chunk);
    }

    public List<Chunk> getChunks() {
        return new ArrayList<>(chunks);
    }

    public void removeChunk(Chunk chunk) {
        chunks.removeIf(c -> c.getId().equals(chunk.getId()));
    }
}
