package com.andrewaleynik.ragsystem.data.mappers;

import com.andrewaleynik.ragsystem.data.entities.CollectionJpaEntity;
import com.andrewaleynik.ragsystem.domains.CollectionDomain;
import com.andrewaleynik.ragsystem.factories.CollectionFactory;
import org.springframework.stereotype.Component;

@Component
public class CollectionMapper implements Mapper<CollectionJpaEntity, CollectionDomain> {
    @Override
    public CollectionDomain toDomain(CollectionJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return CollectionFactory.from(entity).createDomain();
    }

    @Override
    public CollectionJpaEntity toEntity(CollectionDomain domain) {
        if (domain == null) {
            return null;
        }
        return CollectionFactory.from(domain).createEntity();
    }

    @Override
    public void updateEntity(CollectionDomain domain, CollectionJpaEntity entity) {
        if (domain == null || entity == null) {
            return;
        }

        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setName(domain.getName());
        entity.setIndexedAt(domain.getIndexedAt());
        entity.setDocuments(domain.getDocuments());
        entity.setActive(domain.getActive());
    }
}
