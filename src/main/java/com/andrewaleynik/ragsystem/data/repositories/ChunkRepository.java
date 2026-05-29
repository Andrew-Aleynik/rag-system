package com.andrewaleynik.ragsystem.data.repositories;

import com.andrewaleynik.ragsystem.data.entities.Chunk;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChunkRepository extends CrudRepository<Chunk, Long> {
    List<Chunk> findAllByDocumentId(Long documentId);

    List<Chunk> findAllByVectorIdIn(List<String> vectorIds);

    List<Chunk> findByDocumentIdAndStructural(Long documentId, boolean structural);

    List<Chunk> findByDocumentIdAndStructuralAndIndexBetween(Long documentId, boolean structural,
                                                             int startIndex, int endIndex);
}
