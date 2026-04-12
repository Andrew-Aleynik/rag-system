package com.andrewaleynik.ragsystem.data.entities;

import com.andrewaleynik.ragsystem.data.ChunkData;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity(name = "Chunks")
@Table(name = "chunks")
@Data
public class ChunkJpaEntity implements ChunkData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column
    private String vectorId;
    @Column(name = "document_id")
    private Long documentId;
    @Column
    private Boolean structural;
    @Column
    private String content;
    @Column
    private String hash;
    @Column
    private Integer index;
    @Column
    private Integer sizeBytes;
    @Column
    private Integer sizeTokens;
    @Column
    private LocalDateTime createdAt;
    @Column
    private LocalDateTime indexedAt;
}
