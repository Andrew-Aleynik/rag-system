package com.andrewaleynik.ragsystem.app.controllers;

import com.andrewaleynik.ragsystem.app.dto.project.request.collection.*;
import com.andrewaleynik.ragsystem.app.dto.project.response.CollectionListResponse;
import com.andrewaleynik.ragsystem.app.dto.project.response.CollectionResponse;
import com.andrewaleynik.ragsystem.app.dto.project.response.DocumentListResponse;
import com.andrewaleynik.ragsystem.app.dto.project.response.TaskStatusResponse;
import com.andrewaleynik.ragsystem.app.services.CollectionCrudService;
import com.andrewaleynik.ragsystem.app.services.core.CollectionIndexService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/collections")
@Tag(name = "Collection Management", description = "Endpoints for managing collections")
public class CollectionController {

    private final CollectionCrudService collectionCrudService;
    private final CollectionIndexService collectionIndexService;

    @PostMapping
    @Operation(summary = "Create a new collection", description = "Creates a new collection with the provided details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Collection created successfully",
                    content = @Content(schema = @Schema(implementation = CollectionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "409", description = "Collection already exists")
    })
    public ResponseEntity<CollectionResponse> createCollection(
            @Valid @RequestBody CollectionCreateRequest request
    ) {
        log.info("Creating new collection with name: {}", request.name());
        CollectionResponse response = collectionCrudService.createCollection(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all collections", description = "Retrieves a list of all collections")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Collections retrieved successfully",
                    content = @Content(schema = @Schema(implementation = CollectionListResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<CollectionListResponse> getAllCollections(
            @Valid CollectionRetrieveRequest request
    ) {
        log.info("Retrieving all collections");
        CollectionListResponse response = collectionCrudService.retrieveCollections(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get collection by ID", description = "Retrieves a single collection by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Collection found",
                    content = @Content(schema = @Schema(implementation = CollectionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Collection not found")
    })
    public ResponseEntity<CollectionResponse> getCollectionById(
            @Parameter(description = "Collection ID", required = true)
            @PathVariable Long id
    ) {
        log.info("Retrieving collection with id: {}", id);
        CollectionRetrieveRequest request = new CollectionRetrieveRequest();
        CollectionListResponse response = collectionCrudService.retrieveCollections(request);

        if (response.collections().isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(response.collections().get(0));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update collection", description = "Updates an existing collection")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Collection updated successfully",
                    content = @Content(schema = @Schema(implementation = CollectionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "404", description = "Collection not found")
    })
    public ResponseEntity<CollectionResponse> updateCollection(
            @Parameter(description = "Collection ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody CollectionUpdateRequest request
    ) {
        log.info("Updating collection with id: {}", id);
        CollectionUpdateRequest updateRequest = new CollectionUpdateRequest(
                id,
                request.name(),
                request.active()
        );
        CollectionResponse response = collectionCrudService.updateCollection(updateRequest);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete collection", description = "Deletes a collection by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Collection deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Collection not found")
    })
    public ResponseEntity<Void> deleteCollection(
            @Parameter(description = "Collection ID", required = true)
            @PathVariable Long id
    ) {
        log.info("Deleting collection with id: {}", id);
        CollectionDeleteRequest request = new CollectionDeleteRequest(id);
        collectionCrudService.deleteCollection(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/documents")
    @Operation(summary = "Add documents to collection", description = "Adds multiple documents to the collection")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Documents added successfully",
                    content = @Content(schema = @Schema(implementation = DocumentListResponse.class))),
            @ApiResponse(responseCode = "404", description = "Collection or documents not found"),
            @ApiResponse(responseCode = "409", description = "Some documents already in collection")
    })
    public ResponseEntity<DocumentListResponse> addDocumentsToCollection(
            @Parameter(description = "Collection ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody CollectionAddDocumentRequest request
    ) {
        log.info("Adding {} documents to collection {}",
                request.documentIds() != null ? request.documentIds().size() : 0, id);
        DocumentListResponse response = collectionCrudService.addDocumentsToCollection(id, request.documentIds());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/documents")
    @Operation(summary = "Remove documents from collection", description = "Removes multiple documents from the collection")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Documents removed successfully",
                    content = @Content(schema = @Schema(implementation = DocumentListResponse.class))),
            @ApiResponse(responseCode = "404", description = "Collection or documents not found")
    })
    public ResponseEntity<DocumentListResponse> removeDocumentsFromCollection(
            @Parameter(description = "Collection ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody CollectionRemoveDocumentRequest request
    ) {
        log.info("Removing {} documents from collection {}", request.documentIds().size(), id);
        DocumentListResponse response = collectionCrudService.removeDocumentsFromCollection(id, request.documentIds());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/documents")
    @Operation(summary = "Get collection documents", description = "Retrieves all documents in the collection")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Documents retrieved successfully",
                    content = @Content(schema = @Schema(implementation = DocumentListResponse.class))),
            @ApiResponse(responseCode = "404", description = "Collection not found")
    })
    public ResponseEntity<DocumentListResponse> getCollectionDocuments(
            @Parameter(description = "Collection ID", required = true)
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Retrieving documents for collection {} (page: {}, size: {})", id, page, size);
        DocumentListResponse response = collectionCrudService.getCollectionDocuments(id, page, size);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/index")
    @Operation(summary = "Index collection", description = "Starts indexing of collection documents for RAG")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Index task queued successfully",
                    content = @Content(schema = @Schema(implementation = TaskStatusResponse.class))),
            @ApiResponse(responseCode = "404", description = "Collection not found"),
            @ApiResponse(responseCode = "409", description = "Index already in progress")
    })
    public ResponseEntity<TaskStatusResponse> indexCollection(
            @Parameter(description = "Collection ID", required = true)
            @PathVariable Long id
    ) {
        log.info("Starting indexing for collection with id: {}", id);
        CollectionIndexRequest request = new CollectionIndexRequest(id);
        TaskStatusResponse response = collectionIndexService.tryStartIndexCollection(request);

        HttpStatus status = switch (response.status()) {
            case QUEUED -> HttpStatus.ACCEPTED;
            case REJECTED -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };

        return ResponseEntity.status(status).body(response);
    }
}