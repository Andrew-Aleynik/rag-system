package com.andrewaleynik.ragsystem.app.services.core;

import com.andrewaleynik.ragsystem.config.ExVectorStore;
import com.google.common.collect.Lists;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class QdrantVectorStoreHelper implements VectorStoreHelper {
    @Override
    public void deleteVectorsFromVectorStore(ExVectorStore vectorStore, List<String> vectorIds) {
        if (vectorIds.isEmpty()) return;
        vectorStore.vectorStore().delete(vectorIds);
    }

    @Override
    public List<Points.PointStruct> fetchVectorsFromVectorStore(ExVectorStore vectorStore, List<String> vectorIds) {
        if (vectorIds.isEmpty()) {
            return Collections.emptyList();
        }

        QdrantClient qdrantClient = (QdrantClient) vectorStore.vectorStore().getNativeClient().get();
        List<Points.PointStruct> result = new ArrayList<>();

        for (List<String> batch : Lists.partition(vectorIds, 100)) {
            List<Points.PointId> pointIds = batch.stream()
                    .map(id -> Points.PointId.newBuilder().setUuid(id).build())
                    .toList();

            Points.GetPoints request = Points.GetPoints.newBuilder()
                    .setCollectionName(vectorStore.collectionName())
                    .addAllIds(pointIds)
                    .setWithVectors(Points.WithVectorsSelector.newBuilder().setEnable(true).build())
                    .setWithPayload(Points.WithPayloadSelector.newBuilder().setEnable(true).build())
                    .build();
            List<Points.RetrievedPoint> points;
            try {
                points = qdrantClient.retrieveAsync(request, Duration.ofSeconds(10)).get();
            } catch (Exception e) {
                throw new RuntimeException("Failed to fetch vectors in " + vectorStore.collectionName(), e);
            }
            for (Points.RetrievedPoint point : points) {
                Points.PointStruct.Builder builder = Points.PointStruct.newBuilder()
                        .setId(point.getId())
                        .setVectors(point.getVectors())
                        .putAllPayload(point.getPayloadMap());
                result.add(builder.build());
            }
        }
        return result;
    }

    @Override
    public void writeVectorsToVectorStore(ExVectorStore vectorStore, List<Points.PointStruct> points) {
        if (points.isEmpty()) {
            return;
        }

        QdrantClient qdrantClient = (QdrantClient) vectorStore.vectorStore().getNativeClient().get();
        for (List<Points.PointStruct> pointsPartition : Lists.partition(points, 100)) {
            Points.UpsertPoints request = Points.UpsertPoints.newBuilder()
                    .setCollectionName(vectorStore.collectionName())
                    .addAllPoints(pointsPartition)
                    .build();
            try {
                qdrantClient.upsertAsync(request, Duration.ofSeconds(10)).get();
            } catch (Exception e) {
                throw new RuntimeException("Failed to upsert vectors in " + vectorStore.collectionName(), e);
            }
        }
    }
}
