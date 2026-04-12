package com.andrewaleynik.ragsystem.chunkers;

import com.andrewaleynik.ragsystem.domains.ChunkDomain;
import com.andrewaleynik.ragsystem.domains.DocumentDomain;
import com.andrewaleynik.ragsystem.exceptions.ChunkingException;

import java.io.IOException;
import java.util.List;

public interface Chunker {
    List<ChunkDomain> chunkDocument(DocumentDomain document) throws ChunkingException, IOException;
}
