package com.andrewaleynik.ragsystem.app.dto.request;

import java.util.List;

public record RetrieveRequest(String query, List<String> fileExtensions) {
}
