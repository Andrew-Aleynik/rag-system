package com.andrewaleynik.ragsystem.app.services;

import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class FileHashService {
    public String calculateHash(Path file) throws IOException {
        byte[] content = Files.readAllBytes(file);
        return DigestUtils.md5DigestAsHex(content);
    }
}
