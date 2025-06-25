package com.app.trekha.user.service;

import com.app.trekha.common.exception.ResourceNotFoundException;
import com.app.trekha.storage.FileStorageService;
import com.app.trekha.user.dto.KycDocumentResponse;
import com.app.trekha.user.dto.KycUploadRequest;
import com.app.trekha.user.model.KycDocument;
import com.app.trekha.user.model.KycDocumentType;
import com.app.trekha.user.model.KycStatus;
import com.app.trekha.user.model.User;
import com.app.trekha.user.repository.KycDocumentRepository;
import com.app.trekha.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KycServiceTest {

    @Mock
    private KycDocumentRepository kycDocumentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private KycService kycService;

    private User testUser;
    private KycUploadRequest kycUploadRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");

        MockMultipartFile mockFile = new MockMultipartFile("file", "test-id.jpg", "image/jpeg", "test data".getBytes());
        kycUploadRequest = new KycUploadRequest();
        kycUploadRequest.setDocumentType(KycDocumentType.NATIONAL_ID);
        kycUploadRequest.setFile(mockFile);
        kycUploadRequest.setExpiryDate(LocalDate.of(2030, 12, 31));
    }

    @Test
    void uploadKycDocument_HappyPath_ShouldSucceed() throws IOException {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(fileStorageService.store(any(), anyString())).thenReturn("/kyc-documents/1/unique-file.jpg");
        // Use thenAnswer to return the saved entity with its generated ID
        when(kycDocumentRepository.save(any(KycDocument.class))).thenAnswer(invocation -> {
            KycDocument doc = invocation.getArgument(0);
            doc.setId(100L); // Simulate DB generating an ID
            return doc;
        });

        // Act
        KycDocumentResponse response = kycService.uploadKycDocument(1L, kycUploadRequest);

        // Assert
        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(KycStatus.PENDING, response.getStatus());
        assertEquals("/kyc-documents/1/unique-file.jpg", response.getDocumentUrl());
        assertEquals(LocalDate.of(2030, 12, 31), response.getExpiryDate());

        // Verify interactions and capture saved entity
        ArgumentCaptor<KycDocument> kycDocumentCaptor = ArgumentCaptor.forClass(KycDocument.class);
        verify(kycDocumentRepository).save(kycDocumentCaptor.capture());
        KycDocument savedDocument = kycDocumentCaptor.getValue();

        assertEquals(testUser, savedDocument.getUser());
        assertEquals(KycDocumentType.NATIONAL_ID, savedDocument.getDocumentType());
        assertNotNull(savedDocument.getUploadedAt());
    }

    @Test
    void uploadKycDocument_WhenUserNotFound_ShouldThrowResourceNotFoundException() throws IOException{
        // Arrange
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> kycService.uploadKycDocument(99L, kycUploadRequest));
        verify(fileStorageService, never()).store(any(), anyString());
        verify(kycDocumentRepository, never()).save(any());
    }

    @Test
    void uploadKycDocument_WhenFileStorageFails_ShouldThrowRuntimeException() throws IOException {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(fileStorageService.store(any(), anyString())).thenThrow(new IOException("Disk is full"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> kycService.uploadKycDocument(1L, kycUploadRequest));
        assertEquals("Failed to store KYC document file.", exception.getMessage());
        verify(kycDocumentRepository, never()).save(any());
    }

    @Test
    void uploadKycDocument_WithNullRequest_ShouldThrowNullPointerException() {
        // Arrange
        // We need to mock the user lookup to avoid a ResourceNotFoundException
        // before the NullPointerException can occur.
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(NullPointerException.class, () -> kycService.uploadKycDocument(1L, null));
    }
}