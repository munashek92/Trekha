package com.app.trekha.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;



public interface FileStorageService {
    void init(); // Initialize storage location
    String store(MultipartFile file, String subDirectory) throws IOException; // Returns the path or URL

}
