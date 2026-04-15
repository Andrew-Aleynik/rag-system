package com.andrewaleynik.ragsystem.app.services;

import com.andrewaleynik.ragsystem.app.dto.project.request.project.ProjectSyncRequest;
import com.andrewaleynik.ragsystem.app.services.core.ProjectSyncService;
import com.andrewaleynik.ragsystem.app.services.core.TaskService;
import com.andrewaleynik.ragsystem.config.TestConfig;
import com.andrewaleynik.ragsystem.data.entities.ProjectJpaEntity;
import com.andrewaleynik.ragsystem.data.repositories.ProjectRepository;
import com.andrewaleynik.ragsystem.domains.ProjectType;
import com.andrewaleynik.ragsystem.domains.Task;
import com.andrewaleynik.ragsystem.domains.TaskId;
import com.andrewaleynik.ragsystem.domains.TaskStatus;
import com.andrewaleynik.ragsystem.factories.ProjectFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
@Import(TestConfig.class)
class ProjectSyncServiceIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("services.task.cleanup_period_millis", () -> 200);
    }

    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private ProjectSyncService projectSyncService;
    @Autowired
    private TaskService taskService;

    @Test
    void testTryStartSyncProject(@TempDir Path tempDir) {
        ProjectJpaEntity entity = new ProjectFactory()
                .withUrl("https://github.com/githubtraining/hellogitworld")
                .withType(ProjectType.GITHUB)
                .withName("some_project")
                .withDefaultBranch("master")
                .withLocalPath(tempDir.toString())
                .createEntity();
        projectRepository.save(entity);
        ProjectSyncRequest request = new ProjectSyncRequest(entity.getId());

        projectSyncService.tryStartSyncProject(request);

        TaskId taskId = taskService.getTaskId(entity);

        assertTrue(taskService.contains(taskId));

        await().atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(2))
                .until(() -> {
                    Optional<Task> task = taskService.getTask(taskId);
                    if (task.isEmpty()) return false;

                    TaskStatus status = task.get().getStatus();
                    System.out.println("Status: " + status);

                    return status == TaskStatus.DONE || status == TaskStatus.FAILED;
                });

        ProjectJpaEntity updated = projectRepository.findById(entity.getId()).orElseThrow();
        assertNotNull(updated.getSyncedAt());
    }
}
