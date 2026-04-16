package com.andrewaleynik.ragsystem.data.repositories;

import com.andrewaleynik.ragsystem.data.entities.CollectionJpaEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface CollectionRepository extends CrudRepository<CollectionJpaEntity, Long> {
    List<CollectionJpaEntity> getAllByActive(boolean active);

}
