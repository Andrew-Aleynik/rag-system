package com.andrewaleynik.ragsystem.app.dto.response;

import com.andrewaleynik.ragsystem.data.entities.Chunk;

import java.util.List;

public record ChunkDifference(
        List<Chunk> newChunks,
        List<Chunk> deletedChunks,
        List<Chunk> unchangedChunks
) {
}
