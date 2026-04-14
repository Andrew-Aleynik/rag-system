package com.andrewaleynik.ragsystem.app.dto.project.request;

import com.andrewaleynik.ragsystem.domains.ProjectDomain;

import java.util.List;

public record RetrieveRequest(String query, List<ProjectDomain> projects) {
}
