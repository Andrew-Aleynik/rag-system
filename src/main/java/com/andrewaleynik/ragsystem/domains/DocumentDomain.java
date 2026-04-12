package com.andrewaleynik.ragsystem.domains;

import com.andrewaleynik.ragsystem.data.ChunkData;
import com.andrewaleynik.ragsystem.data.DocumentData;
import lombok.Data;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class DocumentDomain implements DocumentData {
    private Long id;
    private Long projectId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Path localPath;
    private String fileName;
    private String fileExtension;
    private String fileHash;
    private LocalDateTime indexedAt;
    private List<ChunkData> chunks = new ArrayList<>();


    public Path getLocalPathAsPath() {
        return localPath;
    }

    @Override
    public void setLocalPath(String localPath) {
        this.localPath = localPath != null ? Paths.get(localPath) : null;
    }

    @Override
    public String getLocalPath() {
        return localPath != null ? localPath.toString() : null;
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
        chunks.add(chunk);
    }

    @Override
    public void removeChunk(ChunkData chunk) {
        if (chunk != null) {
            chunks.removeIf(c -> c.getId().equals(chunk.getId()));
        }
    }
}
