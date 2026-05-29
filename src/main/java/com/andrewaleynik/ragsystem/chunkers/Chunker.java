package com.andrewaleynik.ragsystem.chunkers;

import com.andrewaleynik.ragsystem.data.entities.Chunk;
import com.andrewaleynik.ragsystem.data.entities.Document;
import com.andrewaleynik.ragsystem.exceptions.ChunkingException;

import java.io.IOException;
import java.util.List;

public interface Chunker {
    List<Chunk> chunkDocument(Document document) throws ChunkingException, IOException;
}
