package com.andrewaleynik.ragsystem.data.repositories;

import com.andrewaleynik.ragsystem.data.entities.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends CrudRepository<Document, Long> {
    List<Document> findAllByProjectId(Long projectId);

    Optional<Document> findByProjectIdAndLocalPath(Long projectId, String localPath);

    @Query("SELECT d FROM Document d JOIN d.collections c WHERE c.id = :collectionId")
    Page<Document> findByCollectionId(@Param("collectionId") Long collectionId, Pageable pageable);

    @Query("SELECT d FROM Document d WHERE d.projectId = :projectId")
    Page<Document> findByProjectId(@Param("projectId") Long projectId, Pageable pageable);
}
