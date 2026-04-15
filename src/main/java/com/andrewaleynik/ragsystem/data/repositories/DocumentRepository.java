package com.andrewaleynik.ragsystem.data.repositories;

import com.andrewaleynik.ragsystem.data.entities.DocumentJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends CrudRepository<DocumentJpaEntity, Long> {
    List<DocumentJpaEntity> findAllByProjectId(Long projectId);

    Optional<DocumentJpaEntity> findByProjectIdAndLocalPath(Long projectId, String localPath);

    @Query("SELECT d FROM Document d JOIN d.collections c WHERE c.id = :collectionId")
    Page<DocumentJpaEntity> findByCollectionId(@Param("collectionId") Long collectionId, Pageable pageable);
}
