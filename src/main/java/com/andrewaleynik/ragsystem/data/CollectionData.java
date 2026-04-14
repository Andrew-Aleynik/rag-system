package com.andrewaleynik.ragsystem.data;

import java.time.LocalDateTime;

public interface CollectionData extends DocumentContainer, Named {
    Long getId();

    void setId(Long id);

    LocalDateTime getCreatedAt();

    void setCreatedAt(LocalDateTime createdAt);

    LocalDateTime getUpdatedAt();

    void setUpdatedAt(LocalDateTime updatedAt);

    LocalDateTime getIndexedAt();

    void setIndexedAt(LocalDateTime indexedAt);

    Boolean getActive();

    void setActive(Boolean active);
}
