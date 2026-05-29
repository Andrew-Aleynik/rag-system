package com.andrewaleynik.ragsystem.app.services.core;

import com.andrewaleynik.ragsystem.config.ExVectorStore;
import io.qdrant.client.grpc.Points;

import java.util.List;

public interface VectorStoreHelper {
    void deleteVectorsFromVectorStore(ExVectorStore vectorStore, List<String> vectorIds);

    List<Points.PointStruct> fetchVectorsFromVectorStore(ExVectorStore vectorStore, List<String> vectorIds);

    void writeVectorsToVectorStore(ExVectorStore vectorStore, List<Points.PointStruct> points);
}
