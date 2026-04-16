package com.andrewaleynik.ragsystem.domains;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ProjectType {
    GIT("git");
    private final String name;

    public static ProjectType fromName(String name) {
        for (ProjectType type : values()) {
            if (type.name.equalsIgnoreCase(name)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown type: " + name);
    }
}
