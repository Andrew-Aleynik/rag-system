package com.andrewaleynik.ragsystem.domains;

import com.andrewaleynik.ragsystem.data.ChunkData;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChunkDomain implements ChunkData {
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
}
