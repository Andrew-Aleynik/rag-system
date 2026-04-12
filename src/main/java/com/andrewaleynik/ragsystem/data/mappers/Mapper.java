package com.andrewaleynik.ragsystem.data.mappers;

public interface Mapper<E, D> {
    D toDomain(E entity);
    E toEntity(D domain);
    void updateEntity(D domain, E entity);
}
