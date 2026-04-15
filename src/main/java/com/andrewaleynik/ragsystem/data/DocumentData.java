package com.andrewaleynik.ragsystem.data;

import java.time.LocalDateTime;
import java.util.List;

public interface DocumentData extends Entity{
    void setProjectId(Long projectId);

    Long getProjectId();

    void setCreatedAt(LocalDateTime createdAt);

    LocalDateTime getCreatedAt();

    void setUpdatedAt(LocalDateTime updatedAt);

    LocalDateTime getUpdatedAt();

    void setLocalPath(String localPath);

    String getLocalPath();

    void setFileName(String fileName);

    String getFileName();

    void setFileExtension(String fileExtension);

    String getFileExtension();

    void setFileHash(String fileHash);

    String getFileHash();

    void setIndexedAt(LocalDateTime indexedAt);

    LocalDateTime getIndexedAt();

    void setChunks(List<ChunkData> chunks);

    void addChunk(ChunkData chunk);

    void removeChunk(ChunkData chunk);

    List<ChunkData> getChunks();
}
