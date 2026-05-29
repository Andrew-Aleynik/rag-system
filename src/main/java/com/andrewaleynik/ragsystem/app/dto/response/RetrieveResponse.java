package com.andrewaleynik.ragsystem.app.dto.response;

import com.andrewaleynik.ragsystem.data.entities.Chunk;

import java.util.List;
import java.util.Map;

public record RetrieveResponse(Map<String, List<Chunk>> localPathChunks) {
}
