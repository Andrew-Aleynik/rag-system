package com.andrewaleynik.ragsystem.data.repositories;

import com.andrewaleynik.ragsystem.data.entities.Collection;
import com.andrewaleynik.ragsystem.data.entities.Document;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface CollectionRepository extends CrudRepository<Collection, Long> {
    List<Collection> findAllByDocumentsContains(Document document);

    List<Collection> getAllByActive(boolean active);

}
