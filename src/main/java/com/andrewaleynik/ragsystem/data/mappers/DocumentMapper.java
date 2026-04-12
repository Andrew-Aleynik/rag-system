package com.andrewaleynik.ragsystem.data.mappers;

import com.andrewaleynik.ragsystem.data.entities.DocumentJpaEntity;
import com.andrewaleynik.ragsystem.domains.DocumentDomain;
import com.andrewaleynik.ragsystem.factories.DocumentFactory;
import org.springframework.stereotype.Component;

@Component
public class DocumentMapper implements Mapper<DocumentJpaEntity, DocumentDomain> {
    @Override
    public DocumentDomain toDomain(DocumentJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return DocumentFactory.from(entity).createDomain();
    }

    @Override
    public DocumentJpaEntity toEntity(DocumentDomain domain) {
        if (domain == null) {
            return null;
        }
        return DocumentFactory.from(domain).createEntity();
    }

    @Override
    public void updateEntity(DocumentDomain domain, DocumentJpaEntity entity) {
        if (domain == null || entity == null) {
            return;
        }
        entity.setProjectId(domain.getProjectId());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setLocalPath(domain.getLocalPath());
        entity.setFileName(domain.getFileName());
        entity.setFileExtension(domain.getFileExtension());
        entity.setFileHash(domain.getFileHash());
        entity.setIndexedAt(domain.getIndexedAt());
        entity.setChunks(domain.getChunks());
    }
}
