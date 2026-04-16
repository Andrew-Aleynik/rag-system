package com.andrewaleynik.ragsystem.data.entities;

import com.andrewaleynik.ragsystem.data.ChunkData;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity(name = "Chunks")
@Table(name = "chunks", indexes = {
        @Index(name = "idx_chunk_document_id", columnList = "document_id"),
        @Index(name = "idx_chunk_vector_id", columnList = "vector_id"),
        @Index(name = "idx_chunk_structural", columnList = "structural"),
})
@Data
@ToString(exclude = "document")
public class ChunkJpaEntity implements ChunkData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id")
    private Long documentId;

    @Column(nullable = false)
    private String vectorId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 64, nullable = false)
    private String hash;

    @Column(name = "chunk_index", nullable = false)
    private Integer index;

    @Column(nullable = false)
    private Integer sizeBytes;

    @Column(nullable = false)
    private Integer sizeTokens;

    @Column(nullable = false)
    private Boolean structural;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime indexedAt;

    @PrePersist
    protected void onCreate() {
        if (structural == null) {
            structural = false;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (indexedAt == null) {
            indexedAt = LocalDateTime.now();
        }
    }
}