package com.andrewaleynik.ragsystem.app.services.core;

import com.andrewaleynik.ragsystem.data.entities.DocumentJpaEntity;
import com.andrewaleynik.ragsystem.data.mappers.DocumentMapper;
import com.andrewaleynik.ragsystem.data.repositories.DocumentRepository;
import com.andrewaleynik.ragsystem.domains.DocumentDomain;
import com.andrewaleynik.ragsystem.domains.ProjectDomain;
import com.andrewaleynik.ragsystem.factories.DocumentFactory;
import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
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
    private final DocumentMapper documentMapper;
    private final FileHashService fileHashService;

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "java", "md", "txt", "xml", "yml", "yaml", "json", "properties", "sql"
    );

    public void syncProject(ProjectDomain domain) throws GitAPIException, IOException {
        Path localPath = domain.getLocalPathAsPath();
        if (!Files.exists(localPath.resolve(".git"))) {
            clone(domain.getUrl(), localPath, domain.getDefaultBranch());
        } else {
            pull(localPath);
        }
    }

    public void updateRepositoryInfo(ProjectDomain project) throws IOException {
        Path localPath = project.getLocalPathAsPath();

        try (Git git = Git.open(localPath.toFile());
             Repository repository = git.getRepository()) {
            updateProjectMetadata(project, repository);
            scanAndUpdateDocuments(project, repository);
        }
    }

    private void updateProjectMetadata(ProjectDomain project, Repository repository)
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

    private void scanAndUpdateDocuments(ProjectDomain project, Repository repository)
            throws IOException {

        ObjectId head = repository.resolve(Constants.HEAD);
        Map<String, DocumentJpaEntity> existingDocs = loadExistingDocuments(project.getId());
        Map<String, DocumentJpaEntity> updatedDocs = new HashMap<>();

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
                Path fullPath = project.getLocalPathAsPath().resolve(relativePath);

                DocumentJpaEntity document = existingDocs.getOrDefault(
                        relativePath,
                        createNewDocument(project.getId(), relativePath, fullPath)
                );

                updateDocumentIfChanged(document, fullPath);
                updatedDocs.put(relativePath, document);
            }
        }

        List<DocumentJpaEntity> toDelete = existingDocs.values().stream()
                .filter(doc -> !updatedDocs.containsKey(doc.getLocalPath()))
                .toList();

        if (!toDelete.isEmpty()) {
            documentRepository.deleteAll(toDelete);
        }
        documentRepository.saveAll(updatedDocs.values());
    }

    private Map<String, DocumentJpaEntity> loadExistingDocuments(Long projectId) {
        return documentRepository.findAllByProjectId(projectId).stream()
                .collect(Collectors.toMap(
                        DocumentJpaEntity::getLocalPath,
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

    private DocumentJpaEntity createNewDocument(Long projectId, String relativePath, Path fullPath) {
        String fileName = fullPath.getFileName().toString();

        return new DocumentFactory()
                .withProjectId(projectId)
                .withLocalPath(relativePath)
                .withFileName(fileName)
                .withFileExtension(getFileExtension(fileName))
                .withFileHash("")
                .withCreatedAt(LocalDateTime.now())
                .withUpdatedAt(LocalDateTime.now())
                .createEntity();
    }

    private void updateDocumentIfChanged(DocumentJpaEntity document, Path fullPath) {
        try {
            String currentHash = fileHashService.calculateHash(fullPath);
            String storedHash = document.getFileHash();

            if (!currentHash.equals(storedHash)) {
                DocumentDomain domain = DocumentFactory.from(document).createDomain();

                domain.setFileHash(currentHash);
                domain.setUpdatedAt(LocalDateTime.now());

                documentMapper.updateEntity(domain, document);
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

    private void clone(String url, Path target, String branch) throws GitAPIException {
        Git.cloneRepository()
                .setURI(url)
                .setDirectory(target.toFile())
                .setBranch(branch)
                .call();
    }

    private void pull(Path localPath) throws GitAPIException, IOException {
        try (Git git = Git.open(localPath.toFile())) {
            git.pull().call();
        }
    }
}
