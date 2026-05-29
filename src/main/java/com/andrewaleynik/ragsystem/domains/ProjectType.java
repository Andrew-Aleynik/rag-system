package com.andrewaleynik.ragsystem.domains;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ProjectType {
    GIT("git");
    private final String sourceName;

    public static ProjectType of(String sourceName) {
        for (ProjectType type : values()) {
            if (type.sourceName.equalsIgnoreCase(sourceName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown type: " + sourceName);
    }
}
