package com.andrewaleynik.ragsystem.data.mappers;

import com.andrewaleynik.ragsystem.data.entities.ProjectJpaEntity;
import com.andrewaleynik.ragsystem.domains.ProjectDomain;
import com.andrewaleynik.ragsystem.factories.ProjectFactory;
import org.springframework.stereotype.Component;

@Component
public class ProjectMapper implements Mapper<ProjectJpaEntity, ProjectDomain> {
    @Override
    public ProjectDomain toDomain(ProjectJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return ProjectFactory.from(entity).createDomain();
    }

    @Override
    public ProjectJpaEntity toEntity(ProjectDomain domain) {
        if (domain == null) {
            return null;
        }
        return ProjectFactory.from(domain).createEntity();
    }

    @Override
    public void updateEntity(ProjectDomain domain, ProjectJpaEntity entity) {
        if (domain == null || entity == null) {
            return;
        }

        entity.setDefaultBranch(domain.getDefaultBranch());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setLocalPath(domain.getLocalPath());
        entity.setName(domain.getName());
        entity.setType(domain.getType());
        entity.setSyncedAt(domain.getSyncedAt());
        entity.setIndexedAt(domain.getIndexedAt());
        entity.setDocuments(domain.getDocuments());
        entity.setActive(domain.getActive());
    }
}
