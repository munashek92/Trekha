package com.app.trekha.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LocalFileStorageServiceTest {

    private LocalFileStorageService storageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        storageService = new LocalFileStorageService();
   // Set the physical root location to the temporary directory for tests
        ReflectionTestUtils.setField(storageService, "rootLocation", tempDir);
        // Set the storageLocationName (used for URL prefix) to a dummy name for tests
        ReflectionTestUtils.setField(storageService, "storageLocationName", "test-uploads");
        // Ensure the tempDir itself exists (it's usually managed by @TempDir, but explicit create is safer)
        try {
            Files.createDirectories(tempDir);
        } catch (IOException e) {
            fail("Failed to ensure temp directory exists: " + e.getMessage());
        }
    }

    @Test
    void store_ValidFile_ShouldStoreFileAndReturnPath() throws IOException {
        MultipartFile file = new MockMultipartFile("test.txt", "test.txt", "text/plain", "Hello, world!".getBytes());
        String subDirectory = "test-subdir";

        String storedFilePath = storageService.store(file, subDirectory);

       // Verify the URL path format
        String expectedUrlPrefix = "/" + ReflectionTestUtils.getField(storageService, "storageLocationName") + "/" + subDirectory + "/";
        assertTrue(storedFilePath.startsWith(expectedUrlPrefix), "Returned path should start with expected URL prefix");
        assertTrue(storedFilePath.endsWith(".txt"), "Returned path should end with .txt extension");
    
        // Extract the unique file name from the returned path
        String uniqueFileName = storedFilePath.substring(storedFilePath.lastIndexOf('/') + 1);
    
        // Verify the physical file is stored and its content
        Path storedFileFullPath = tempDir.resolve(subDirectory).resolve(uniqueFileName);
        assertTrue(Files.exists(storedFileFullPath), "Physical file should exist");
        assertEquals("Hello, world!", Files.readString(storedFileFullPath), "Stored file content should match");
    
    }

    @Test
    void store_EmptyFile_ShouldThrowIOException() {
        MultipartFile emptyFile = new MockMultipartFile("empty.txt", new byte[0]);
        String subDirectory = "test-subdir";

        assertThrows(IOException.class, () -> storageService.store(emptyFile, subDirectory));
    }

    @Test
    void store_NoSubDirectory_ShouldStoreInRoot() throws IOException {
        MultipartFile file = new MockMultipartFile("test.txt", "test.txt", "text/plain", "Root file".getBytes());

        String subDirectory = ""; // No sub-directory

        String storedFilePath = storageService.store(file, "");

// Verify the URL path format
        String expectedUrlPrefix = "/" + ReflectionTestUtils.getField(storageService, "storageLocationName") + "/";
        assertTrue(storedFilePath.startsWith(expectedUrlPrefix), "Returned path should start with expected URL prefix");
        assertTrue(storedFilePath.endsWith(".txt"), "Returned path should end with .txt extension");
    
        // Extract the unique file name
        String uniqueFileName = storedFilePath.substring(storedFilePath.lastIndexOf('/') + 1);
        Path storedFileFullPath = tempDir.resolve(uniqueFileName); // Directly in tempDir
        assertTrue(Files.exists(storedFileFullPath), "Physical file should exist in root temp directory");
        assertEquals("Root file", Files.readString(storedFileFullPath), "Stored file content should match");
    
    }

    @Test
    void store_WithFileExtension_ShouldPreserveExtension() throws IOException {
        MultipartFile file = new MockMultipartFile("image.jpg", "image.jpg", "image/jpeg", "Image data".getBytes());
        String subDirectory = "images";

        String storedFilePath = storageService.store(file, subDirectory);

        assertTrue(storedFilePath.endsWith(".jpg"), "Returned path should end with .jpg extension");
    
        String uniqueFileName = storedFilePath.substring(storedFilePath.lastIndexOf('/') + 1);
        Path storedFileFullPath = tempDir.resolve(subDirectory).resolve(uniqueFileName);
        assertTrue(Files.exists(storedFileFullPath), "Physical file should exist");
    
    }

    @Test
    void store_SubDirectoryAlreadyExists_ShouldStoreSuccessfully() throws IOException {
        // Create the subdirectory beforehand
        String subDirectory = "existing-dir";
        Files.createDirectories(tempDir.resolve(subDirectory));

        MultipartFile file = new MockMultipartFile("file.txt", "file.txt", "text/plain", "Test file".getBytes());
        String storedFilePath = storageService.store(file, subDirectory);

        String expectedUrlPrefix = "/" + ReflectionTestUtils.getField(storageService, "storageLocationName") + "/" + subDirectory + "/";
        assertTrue(storedFilePath.startsWith(expectedUrlPrefix), "Returned path should start with expected URL prefix");
    
        String uniqueFileName = storedFilePath.substring(storedFilePath.lastIndexOf('/') + 1);
        Path storedFileFullPath = tempDir.resolve(subDirectory).resolve(uniqueFileName);
        assertTrue(Files.exists(storedFileFullPath), "Physical file should exist");
        assertEquals("Test file", Files.readString(storedFileFullPath), "Stored file content should match");
    
    }

    @Test
    void store_FileWithComplexName_ShouldStoreSuccessfully() throws IOException {
        MultipartFile file = new MockMultipartFile("file with spaces and !@#$%^&().txt", "file with spaces and !@#$%^&().txt", "text/plain", "Complex name".getBytes());
        String subDirectory = "complex";

        String storedFilePath = storageService.store(file, subDirectory);

        // Verify URL format (optional, but good to check it doesn't break)
        String expectedUrlPrefix = "/" + ReflectionTestUtils.getField(storageService, "storageLocationName") + "/" + subDirectory + "/";
        assertTrue(storedFilePath.startsWith(expectedUrlPrefix), "Returned path should start with expected URL prefix");
        assertTrue(storedFilePath.endsWith(".txt"), "Returned path should end with .txt extension");
    
        String uniqueFileName = storedFilePath.substring(storedFilePath.lastIndexOf('/') + 1);
        Path storedFileFullPath = tempDir.resolve(subDirectory).resolve(uniqueFileName);
        assertTrue(Files.exists(storedFileFullPath), "Physical file should exist");
        assertEquals("Complex name", Files.readString(storedFileFullPath), "Stored file content should match");
    
    }

    @Test
    void init_shouldCreateStorageDirectory() {
        LocalFileStorageService newStorageService = new LocalFileStorageService();
        String testStorageLocationName = "init-test-dir-" + UUID.randomUUID().toString(); // Use unique name
        ReflectionTestUtils.setField(newStorageService, "storageLocationName", testStorageLocationName);
    
        // Manually call init()
        newStorageService.init();
    
        // Verify that the directory was created relative to the current working directory
        Path expectedDir = Paths.get(testStorageLocationName);
        assertTrue(Files.exists(expectedDir), "Directory should be created");
        assertTrue(Files.isDirectory(expectedDir), "Path should be a directory");
    
        // Clean up the created directory to avoid leaving artifacts
        try {
            Files.walk(expectedDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
            assertFalse(Files.exists(expectedDir), "Directory should be deleted after cleanup");
        } catch (IOException e) {
            fail("Failed to clean up test directory: " + e.getMessage());
        }
    }

}
