package com.andrewaleynik.ragsystem.data.repositories;

import com.andrewaleynik.ragsystem.data.entities.ChunkJpaEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChunkRepository extends CrudRepository<ChunkJpaEntity, Long> {
    List<ChunkJpaEntity> findAllByDocumentId(Long documentId);

    List<ChunkJpaEntity> findAllByVectorIdIn(List<String> vectorIds);

    List<ChunkJpaEntity> findByDocumentIdAndStructural(Long documentId, boolean structural);

    List<ChunkJpaEntity> findByDocumentIdAndStructuralAndIndexBetween(Long documentId, boolean structural,
                                                                      int startIndex, int endIndex);
}
