package com.andrewaleynik.ragsystem.data.repositories;

import com.andrewaleynik.ragsystem.data.entities.CollectionJpaEntity;
import org.springframework.data.repository.CrudRepository;

public interface CollectionRepository extends CrudRepository<CollectionJpaEntity, Long> {
}
