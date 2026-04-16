package com.andrewaleynik.ragsystem.app.services;

import com.andrewaleynik.ragsystem.app.dto.project.request.project.ProjectCreateRequest;
import com.andrewaleynik.ragsystem.app.dto.project.request.project.ProjectDeleteRequest;
import com.andrewaleynik.ragsystem.app.dto.project.request.project.ProjectUpdateRequest;
import com.andrewaleynik.ragsystem.app.dto.project.response.ProjectListResponse;
import com.andrewaleynik.ragsystem.app.dto.project.response.ProjectResponse;
import com.andrewaleynik.ragsystem.domains.ProjectType;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.qdrant.QdrantContainer;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class ProjectCrudIntegrationTest {
    @MockitoBean
    private EmbeddingModel embeddingModel;
    @Container
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static QdrantContainer qdrantContainer = new QdrantContainer("qdrant/qdrant:v1.9.0")
            .withExposedPorts(6333, 6334);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");

        registry.add("qdrant.host", qdrantContainer::getHost);
        registry.add("qdrant.port", () -> qdrantContainer.getMappedPort(6334));
    }

    @Autowired
    private ProjectCrudService projectCrudService;

    @Test
    void createProject_shouldSaveAndReturnProject() {
        ProjectCreateRequest request = new ProjectCreateRequest(
                "https://github.com/test/repo1.git",
                "main",
                "test-project1",
                ProjectType.GIT
        );

        ProjectResponse response = projectCrudService.createProject(request);

        assertNotNull(response.id());
        assertEquals("test-project1", response.name());
        assertEquals("https://github.com/test/repo1.git", response.url());
        assertEquals("main", response.defaultBranch());
        assertEquals(ProjectType.GIT, response.type());
    }

    @Test
    void retrieveProjects_shouldReturnAllProjects() {
        projectCrudService.createProject(new ProjectCreateRequest(
                "https://github.com/test/repo2.git", "main", "project2", ProjectType.GIT));
        projectCrudService.createProject(new ProjectCreateRequest(
                "https://github.com/test/repo3.git", "main", "project3", ProjectType.GIT));

        ProjectListResponse response = projectCrudService.retrieveProjects();

        assertEquals(2, response.count().intValue());
        assertEquals(2, response.projects().size());
    }

    @Test
    void updateProject_shouldUpdateFields() {
        ProjectResponse created = projectCrudService.createProject(new ProjectCreateRequest(
                "https://github.com/test/repo4.git", "main", "old-name", ProjectType.GIT));

        ProjectUpdateRequest updateRequest = new ProjectUpdateRequest(
                created.id(),
                "new-name",
                "develop"
        );

        ProjectResponse updated = projectCrudService.updateProject(updateRequest);

        assertEquals("new-name", updated.name());
        assertEquals("develop", updated.defaultBranch());
    }

    @Test
    void updateProject_shouldThrow_whenProjectNotFound() {
        ProjectUpdateRequest request = new ProjectUpdateRequest(999L, "name", "branch");

        assertThrows(EntityNotFoundException.class, () -> projectCrudService.updateProject(request));
    }

    @Test
    void deleteProject_shouldRemoveProject() {
        ProjectResponse created = projectCrudService.createProject(new ProjectCreateRequest(
                "https://github.com/test/repo5.git", "main", "to-delete", ProjectType.GIT));

        projectCrudService.deleteProject(new ProjectDeleteRequest(created.id()));

        ProjectListResponse list = projectCrudService.retrieveProjects();
        assertTrue(list.projects().stream()
                .filter(project -> project.id().equals(created.id()))
                .findAny()
                .isEmpty());
    }

    @Test
    void deleteProject_shouldThrow_whenProjectNotFound() {
        assertThrows(EntityNotFoundException.class, () ->
                projectCrudService.deleteProject(new ProjectDeleteRequest(999L)));
    }
}
