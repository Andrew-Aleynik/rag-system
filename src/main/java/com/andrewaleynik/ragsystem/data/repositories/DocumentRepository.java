package com.andrewaleynik.ragsystem.data.repositories;

import com.andrewaleynik.ragsystem.data.entities.DocumentJpaEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends CrudRepository<DocumentJpaEntity, Long> {
    List<DocumentJpaEntity> findAllByProjectId(Long projectId);

    Optional<DocumentJpaEntity> findByProjectIdAndLocalPath(Long projectId, String localPath);
}
