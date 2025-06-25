package com.app.trekha.user.dto;

import com.app.trekha.user.model.KycDocumentType;
import com.app.trekha.user.model.KycStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KycDocumentResponse {
    private Long id;
    private Long userId;
    private KycDocumentType documentType;
    private String documentUrl;
    private KycStatus status;
    private LocalDateTime uploadedAt;
    private LocalDate expiryDate;
    private String rejectionReason;
}

