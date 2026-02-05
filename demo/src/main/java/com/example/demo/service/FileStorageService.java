package com.example.demo.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.service.exception.BusinessException;

@Service
public class FileStorageService {

    private final Path root;

    public FileStorageService(@Value("${app.upload-dir}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.root);
        } catch (IOException ex) {
            throw new BusinessException("Upload directory creation failed");
        }
    }

    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("File is empty");
        }
        String original = sanitizeFilename(file.getOriginalFilename());
        String ext = getExtension(original);
        String stored = UUID.randomUUID().toString() + ext;
        try {
            Files.copy(file.getInputStream(), root.resolve(stored));
        } catch (IOException ex) {
            throw new BusinessException("File save failed");
        }
        return new StoredFile(original, stored);
    }

    public Resource loadAsResource(String storedFilename) {
        try {
            Path file = root.resolve(storedFilename).normalize();
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists()) {
                throw new BusinessException("File not found");
            }
            return resource;
        } catch (Exception ex) {
            throw new BusinessException("File not found");
        }
    }

    public void delete(String storedFilename) {
        try {
            Files.deleteIfExists(root.resolve(storedFilename).normalize());
        } catch (IOException ex) {
            throw new BusinessException("File delete failed");
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "unknown";
        }
        String cleaned = Paths.get(filename).getFileName().toString();
        cleaned = cleaned.replace("\\", "_").replace("/", "_");
        return cleaned;
    }

    private String getExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx >= 0) {
            return filename.substring(idx);
        }
        return "";
    }

    public record StoredFile(String originalFilename, String storedFilename) {}
}
