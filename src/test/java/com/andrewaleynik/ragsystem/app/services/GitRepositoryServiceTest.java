package com.andrewaleynik.ragsystem.app.services;

import com.andrewaleynik.ragsystem.app.services.core.FileHashService;
import com.andrewaleynik.ragsystem.app.services.core.GitRepositoryService;
import com.andrewaleynik.ragsystem.config.VectorStoreConfig;
import com.andrewaleynik.ragsystem.data.entities.Document;
import com.andrewaleynik.ragsystem.data.entities.Project;
import com.andrewaleynik.ragsystem.data.repositories.DocumentRepository;
import com.andrewaleynik.ragsystem.domains.ProjectType;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitRepositoryServiceTest {
    @Mock
    private VectorStoreConfig vectorStoreConfig;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private FileHashService fileHashService;
    @InjectMocks
    private GitRepositoryService gitRepositoryService;

    @Test
    void testSyncExistLocalProject() {
        String repoUri = "https://github.com/githubtraining/hellogitworld";
        String localName = "some_project";
        Path tempDir = cloneRepository(localName, repoUri);
        try {
            Project project = Project.builder()
                    .url(repoUri)
                    .type(ProjectType.GIT)
                    .name(localName)
                    .defaultBranch("master")
                    .localPath(tempDir.toString())
                    .build();
            assertDoesNotThrow(() -> gitRepositoryService.syncProject(project, null, null));
        } finally {
            cleanup(tempDir.toFile());
        }
    }

    @Test
    void testSyncExistRemoteProject(@TempDir Path tempDir) {
        String repoUri = "https://github.com/githubtraining/hellogitworld";

        Project project = Project.builder()
                .url(repoUri)
                .type(ProjectType.GIT)
                .name("some_project")
                .defaultBranch("master")
                .localPath(tempDir.toString())
                .build();

        assertDoesNotThrow(() -> gitRepositoryService.syncProject(project, null, null));
        Assertions.assertTrue(Files.exists(tempDir.resolve(".git")));
    }

    @Test
    void testUpdateRepositoryInfo(@TempDir Path tempDir) throws Exception {
        createLocalRepository(tempDir);
        Project project = Project.builder()
                .id(1L)
                .url("https://github.com/test/test")
                .name("master")
                .type(ProjectType.GIT)
                .defaultBranch("test-project")
                .localPath(tempDir.toString())
                .build();

        when(documentRepository.findAllByProjectId(1L)).thenReturn(Collections.emptyList());
        when(documentRepository.saveAll(anyIterable())).thenReturn(Collections.emptyList());
        when(fileHashService.calculateHash(any(Path.class))).thenReturn("mock-hash-123");

        assertDoesNotThrow(() -> gitRepositoryService.updateRepositoryInfo(project));
        assertNotNull(project.getCreatedAt());
        assertNotNull(project.getUpdatedAt());
        assertEquals("master", project.getDefaultBranch());
        verify(documentRepository).saveAll(argThat(docs -> {
            List<Document> saved = new ArrayList<>();
            docs.forEach(saved::add);
            Document entity = saved.get(0);
            return saved.size() == 1
                    && entity.getLocalPath().equals(tempDir.toString() + "/src/Test.java")
                    && entity.getFileName().equals("Test.java")
                    && entity.getFileExtension().equals("java")
                    && entity.getFileHash().equals("mock-hash-123");
        }));
        verify(documentRepository).findAllByProjectId(1L);
    }

    private static Path resource(String path) {
        try {
            return Paths.get(GitRepositoryService.class.getClassLoader()
                    .getResource(path)
                    .toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid URI for resource: " + path, e);
        } catch (NullPointerException e) {
            throw new RuntimeException("Resource not found: " + path, e);
        }
    }

    private Path cloneRepository(String tempDirName, String repositoryUri) {
        try {
            Path tempDir = Files.createTempDirectory(tempDirName);
            Git.cloneRepository()
                    .setDirectory(tempDir.toFile())
                    .setURI(repositoryUri.trim())
                    .call();
            return tempDir;
        } catch (IOException e) {
            throw new RuntimeException("Can't create tempdir", e);
        } catch (GitAPIException e) {
            throw new RuntimeException("Some problems with git", e);
        }
    }

    private void createLocalRepository(Path tempDir) throws GitAPIException, IOException {
        Git.init().setDirectory(tempDir.toFile()).call();

        Files.createDirectories(tempDir.resolve("src"));

        Path testFile = tempDir.resolve("src/Test.java");
        Files.writeString(testFile, "public class Test {}");

        Git git = Git.open(tempDir.toFile());
        git.add().addFilepattern(".").call();
        git.commit().setMessage("Initial commit").call();
    }

    private void cleanup(File... files) {
        for (File file : files) {
            try {
                FileUtils.deleteDirectory(file);
            } catch (IOException e) {
                throw new RuntimeException("Some error during cleanup", e);
            }
        }
    }
}
