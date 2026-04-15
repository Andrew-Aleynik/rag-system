package com.andrewaleynik.ragsystem.domains;

public record TaskId(
        Class<?> entityClass,
        Long entityId
) {
}
