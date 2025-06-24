package com.app.trekha.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {

    @Value("${trekha.storage.location:uploads}") // Default to 'uploads' directory
    private String storageLocationName;

    private Path rootLocation;

    // This could be called from a @PostConstruct method or init() if needed
    @PostConstruct
    public void init() {
        if (this.rootLocation == null) {
            this.rootLocation = Paths.get(storageLocationName); // Resolves relative to CWD
        }
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage location", e);
        }
    }

    @Override
    public String store(MultipartFile file, String subDirectory) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("Failed to store empty file.");
        }

        // Ensure the subdirectory exists
        Path subDirPath = rootLocation.resolve(subDirectory); // Use rootLocation for physical path
        Files.createDirectories(subDirPath);

        // Generate a unique file name
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

        Path destinationFile = subDirPath.resolve(uniqueFileName).normalize().toAbsolutePath();

        Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);

        return "/" + storageLocationName + "/" + subDirectory + "/" + uniqueFileName; // Return a URL-friendly path
    }
}
