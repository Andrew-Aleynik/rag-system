package com.andrewaleynik.ragsystem.data;

import java.time.LocalDateTime;

public interface ChunkData {
    void setId(Long id);

    Long getId();

    void setVectorId(String vectorId);

    String getVectorId();

    void setDocumentId(Long documentId);

    Long getDocumentId();

    void setStructural(Boolean structural);

    Boolean getStructural();

    void setContent(String content);

    String getContent();

    void setHash(String hash);

    String getHash();

    void setIndex(Integer index);

    Integer getIndex();

    void setSizeBytes(Integer sizeBytes);

    Integer getSizeBytes();

    void setSizeTokens(Integer sizeTokens);

    Integer getSizeTokens();

    void setCreatedAt(LocalDateTime createdAt);

    LocalDateTime getCreatedAt();

    void setIndexedAt(LocalDateTime indexedAt);

    LocalDateTime getIndexedAt();
}
