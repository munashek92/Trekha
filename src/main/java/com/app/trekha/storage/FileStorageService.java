package com.app.trekha.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.springframework.core.io.Resource;
import java.util.stream.Stream;


public interface FileStorageService {
    void init(); // Initialize storage location
    String store(MultipartFile file, String subDirectory) throws IOException; // Returns the path or URL
    // Stream<Path> loadAll(); // To list files
    // Path load(String filename);
    // Resource loadAsResource(String filenameWithSubdirectory);
    // void deleteAll(); // For cleanup
    // void delete(String filenameWithSubdirectory);
}
