package com.app.trekha.user.service;

import com.app.trekha.common.exception.ResourceNotFoundException;
import com.app.trekha.storage.FileStorageService;
import com.app.trekha.user.dto.KycDocumentResponse;
import com.app.trekha.user.dto.KycUploadRequest;
import com.app.trekha.user.model.KycDocument;
import com.app.trekha.user.model.User;
import com.app.trekha.user.repository.KycDocumentRepository;
import com.app.trekha.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class KycService {

    private final KycDocumentRepository kycDocumentRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public KycDocumentResponse uploadKycDocument(Long userId, KycUploadRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        String documentUrl;
        try {
            String subDirectory = "kyc-documents/" + userId;
            documentUrl = fileStorageService.store(request.getFile(), subDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store KYC document file.", e);
        }

        KycDocument kycDocument = new KycDocument();
        kycDocument.setUser(user);
        kycDocument.setDocumentType(request.getDocumentType());
        kycDocument.setDocumentUrl(documentUrl);
        kycDocument.setUploadedAt(LocalDateTime.now());
        kycDocument.setExpiryDate(request.getExpiryDate());
        // Status defaults to PENDING as defined in the KycDocument entity

        KycDocument savedDocument = kycDocumentRepository.save(kycDocument);

        return new KycDocumentResponse(savedDocument.getId(), savedDocument.getUser().getId(), savedDocument.getDocumentType(), savedDocument.getDocumentUrl(), savedDocument.getStatus(), savedDocument.getUploadedAt(), savedDocument.getExpiryDate(), savedDocument.getRejectionReason());
    }
}

