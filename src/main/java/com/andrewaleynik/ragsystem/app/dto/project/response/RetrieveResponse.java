package com.andrewaleynik.ragsystem.app.dto.project.response;

import com.andrewaleynik.ragsystem.data.ChunkData;

import java.util.List;

public record RetrieveResponse(List<ChunkData> chunks) {
}
