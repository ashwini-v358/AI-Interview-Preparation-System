package com.smartinterview.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

/**
 * FileStorageService — saves uploaded multipart files to a local directory.
 *
 * Concepts:
 * - Multipart file upload: HTTP multipart/form-data sends binary file data
 *   alongside other form fields. Spring automatically binds it to MultipartFile.
 * - We generate a UUID-based filename to avoid collisions and path traversal attacks.
 * - Files are stored under `app.storage.upload-dir` (configurable per environment).
 * - In production, this would be replaced by an object storage backend (S3, GCS).
 */
@Slf4j
@Service
public class FileStorageService {

    private final Path uploadRoot;

    public FileStorageService(@Value("${app.storage.upload-dir:./uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadRoot);
            log.info("File storage root initialized at: {}", this.uploadRoot);
        } catch (IOException e) {
            throw new RuntimeException("Could not create file storage directory: " + this.uploadRoot, e);
        }
    }

    /**
     * Stores an uploaded file and returns the unique filename assigned to it.
     *
     * @param file the uploaded MultipartFile.
     * @param subDir a subdirectory under the upload root (e.g., "resumes", "jds").
     * @return the unique stored filename (UUID-prefixed).
     */
    public String storeFile(MultipartFile file, String subDir) {
        String originalName = sanitizeFilename(file.getOriginalFilename());
        String uniqueFilename = UUID.randomUUID() + "_" + originalName;

        try {
            Path targetDir = this.uploadRoot.resolve(subDir);
            Files.createDirectories(targetDir);

            Path targetPath = targetDir.resolve(uniqueFilename);

            // Security: ensure the resolved path is still inside the upload root
            if (!targetPath.toAbsolutePath().startsWith(this.uploadRoot)) {
                throw new IllegalArgumentException("Path traversal attempt detected");
            }

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored file: {} -> {}", originalName, targetPath);
            return subDir + "/" + uniqueFilename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + originalName, e);
        }
    }

    /**
     * Returns the absolute Path for a stored file given its relative stored path.
     */
    public Path resolveFilePath(String storedPath) {
        return this.uploadRoot.resolve(storedPath).normalize();
    }

    /**
     * Strips directory separators from user-supplied filenames to prevent
     * path traversal attacks (e.g., ../../etc/passwd).
     */
    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) return "file.pdf";
        return Paths.get(filename).getFileName().toString()
                .replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
