package com.andrewaleynik.ragsystem.app.services.core;

import com.andrewaleynik.ragsystem.config.ExVectorStore;
import com.andrewaleynik.ragsystem.config.VectorStoreConfig;
import com.andrewaleynik.ragsystem.data.entities.Chunk;
import com.andrewaleynik.ragsystem.data.entities.Document;
import com.andrewaleynik.ragsystem.data.entities.Project;
import com.andrewaleynik.ragsystem.data.repositories.ChunkRepository;
import com.andrewaleynik.ragsystem.data.repositories.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GitRepositoryService {
    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;
    private final FileHashService fileHashService;
    private final VectorStoreConfig vectorStoreConfig;

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "java", "md", "txt", "xml", "yml", "yaml", "json", "properties", "sql"
    );

    public void syncProject(Project project, String username, String password) throws GitAPIException, IOException {
        Path localPath = Path.of(project.getLocalPath());
        if (!Files.exists(localPath.resolve(".git"))) {
            clone(project.getUrl(), localPath, project.getDefaultBranch(), username, password);
        } else {
            pull(localPath, username, password);
        }
    }

    public void updateRepositoryInfo(Project project) throws IOException {
        Path localPath = Path.of(project.getLocalPath());
        try (Git git = Git.open(localPath.toFile());
             Repository repository = git.getRepository()) {
            updateProjectMetadata(project, repository);
            scanAndUpdateDocuments(project, repository);
        }
    }

    private void updateProjectMetadata(Project project, Repository repository)
            throws IOException {

        ObjectId head = repository.resolve(Constants.HEAD);
        if (head == null) {
            return;
        }

        try (RevWalk revWalk = new RevWalk(repository)) {
            RevCommit lastCommit = revWalk.parseCommit(head);
            LocalDateTime lastCommitDate = commitTimeToLocalDateTime(lastCommit);

            revWalk.markStart(lastCommit);
            RevCommit firstCommit = findFirstCommit(revWalk);
            LocalDateTime firstCommitDate = firstCommit != null
                    ? commitTimeToLocalDateTime(firstCommit)
                    : lastCommitDate;

            project.setCreatedAt(firstCommitDate);
            project.setUpdatedAt(lastCommitDate);
            project.setDefaultBranch(repository.getBranch());
        }
    }

    private void scanAndUpdateDocuments(Project project, Repository repository)
            throws IOException {
        ExVectorStore vectorStore = vectorStoreConfig.getOrCreateVectorStore(project);

        ObjectId head = repository.resolve(Constants.HEAD);
        Map<String, Document> existingDocs = loadExistingDocuments(project.getId());
        Map<String, Document> updatedDocs = new HashMap<>();

        try (RevWalk revWalk = new RevWalk(repository);
             TreeWalk treeWalk = new TreeWalk(repository)) {

            RevCommit commit = revWalk.parseCommit(head);
            treeWalk.addTree(commit.getTree());
            treeWalk.setRecursive(true);

            while (treeWalk.next()) {
                if (!isValidFile(treeWalk)) {
                    continue;
                }

                String relativePath = treeWalk.getPathString();
                Path fullPath = Path.of(project.getLocalPath()).resolve(relativePath);

                Document document = existingDocs.getOrDefault(
                        fullPath.toString(),
                        createNewDocument(project.getId(), fullPath)
                );

                updateDocumentIfChanged(document, fullPath);
                updatedDocs.put(fullPath.toString(), document);
            }
        }

        List<Document> toDelete = existingDocs.values().stream()
                .filter(doc -> !updatedDocs.containsKey(doc.getLocalPath()))
                .toList();

        if (!toDelete.isEmpty()) {
            documentRepository.deleteAll(toDelete);
            for (Document documentData : toDelete) {
                List<Chunk> chunks = chunkRepository.findAllByDocumentId(documentData.getId());
                List<Long> ids = chunks.stream()
                        .map(Chunk::getId)
                        .toList();
                List<String> vectorIds = chunks.stream()
                        .map(Chunk::getVectorId)
                        .toList();
                vectorStore.vectorStore().delete(vectorIds);
                chunkRepository.deleteAllById(ids);
            }
        }
        documentRepository.saveAll(updatedDocs.values());
    }

    private Map<String, Document> loadExistingDocuments(Long projectId) {
        return documentRepository.findAllByProjectId(projectId).stream()
                .collect(Collectors.toMap(
                        Document::getLocalPath,
                        Function.identity(),
                        (a, b) -> a
                ));
    }

    private boolean isValidFile(TreeWalk treeWalk) {
        FileMode mode = treeWalk.getFileMode(0);
        if (mode != FileMode.REGULAR_FILE) {
            return false;
        }

        String path = treeWalk.getPathString();
        String extension = getFileExtension(path);

        return SUPPORTED_EXTENSIONS.contains(extension.toLowerCase());
    }

    private Document createNewDocument(Long projectId, Path fullPath) {
        String fileName = fullPath.getFileName().toString();

        return Document.builder()
                .projectId(projectId)
                .localPath(fullPath.toString())
                .fileName(fileName)
                .fileExtension(getFileExtension(fileName))
                .fileHash("")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private void updateDocumentIfChanged(Document document, Path fullPath) {
        try {
            String currentHash = fileHashService.calculateHash(fullPath);
            String storedHash = document.getFileHash();

            if (!currentHash.equals(storedHash)) {
                document.setFileHash(currentHash);
                document.setUpdatedAt(LocalDateTime.now());
            }
        } catch (IOException e) {
            //TODO logging
        }
    }

    private RevCommit findFirstCommit(RevWalk revWalk) {
        RevCommit first = null;
        for (RevCommit commit : revWalk) {
            first = commit;
        }
        return first;
    }

    private LocalDateTime commitTimeToLocalDateTime(RevCommit commit) {
        return Instant.ofEpochSecond(commit.getCommitTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot == -1 ? "" : filename.substring(lastDot + 1);
    }

    private void clone(String url, Path target, String branch, String username, String password) throws GitAPIException {
        CloneCommand command = Git.cloneRepository()
                .setURI(url)
                .setDirectory(target.toFile())
                .setBranch(branch);
        if (username != null) {
            CredentialsProvider credentialsProvider =
                    new UsernamePasswordCredentialsProvider(username, password);
            command = command.setCredentialsProvider(credentialsProvider);
        }
        command.call();
    }

    private void pull(Path localPath, String username, String password) throws GitAPIException, IOException {
        try (Git git = Git.open(localPath.toFile())) {
            PullCommand command = git.pull();
            if (username != null) {
                CredentialsProvider credentialsProvider =
                        new UsernamePasswordCredentialsProvider(username, password);
                command = command.setCredentialsProvider(credentialsProvider);
            }
            command.call();
        }
    }
}
