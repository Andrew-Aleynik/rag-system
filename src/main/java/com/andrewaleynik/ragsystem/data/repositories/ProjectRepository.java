package com.andrewaleynik.ragsystem.data.repositories;

import com.andrewaleynik.ragsystem.data.entities.Collection;
import com.andrewaleynik.ragsystem.data.entities.Document;
import com.andrewaleynik.ragsystem.data.entities.Project;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends CrudRepository<Project, Long> {
    List<Project> getAllByActive(boolean active);

    @Query("""
            SELECT DISTINCT c FROM Collection c
            JOIN c.documents d
            WHERE d.projectId = :projectId
            """)
    List<Collection> findCollections(@Param("projectId") Long projectId);

    @Modifying
    @Query(value = """
            DELETE FROM collection_documents 
            WHERE document_id IN (
                SELECT id FROM documents WHERE project_id = :projectId
            )
            """, nativeQuery = true)
    void unlinkDocumentsFromCollections(@Param("projectId") Long projectId);

    Project findByDocumentsContains(Document document);
}
