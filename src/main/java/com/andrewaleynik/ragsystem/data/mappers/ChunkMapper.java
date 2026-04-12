package com.andrewaleynik.ragsystem.data.mappers;

import com.andrewaleynik.ragsystem.data.entities.ChunkJpaEntity;
import com.andrewaleynik.ragsystem.domains.ChunkDomain;
import com.andrewaleynik.ragsystem.factories.ChunkFactory;
import org.springframework.stereotype.Component;

@Component
public class ChunkMapper implements Mapper<ChunkJpaEntity, ChunkDomain>{
    @Override
    public ChunkDomain toDomain(ChunkJpaEntity entity) {
        return ChunkFactory.from(entity).createDomain();
    }

    @Override
    public ChunkJpaEntity toEntity(ChunkDomain domain) {
        return ChunkFactory.from(domain).createEntity();
    }

    @Override
    public void updateEntity(ChunkDomain domain, ChunkJpaEntity entity) {
        entity.setVectorId(domain.getVectorId());
        entity.setDocumentId(domain.getDocumentId());
        entity.setContent(domain.getContent());
        entity.setHash(domain.getHash());
        entity.setIndex(domain.getIndex());
        entity.setSizeBytes(domain.getSizeBytes());
        entity.setSizeTokens(domain.getSizeTokens());
        entity.setIndexedAt(domain.getIndexedAt());
    }
}
