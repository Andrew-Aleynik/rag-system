package com.andrewaleynik.ragsystem.domains;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TaskStatus {
    NOT_QUEUED(false),
    QUEUED(false),
    REJECTED(true),
    IN_PROCESS(false),
    DONE(true),
    FAILED(true),
    OUTDATED(true);

    private final boolean terminated;
}
