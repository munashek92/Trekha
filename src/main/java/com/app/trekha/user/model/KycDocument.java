package com.app.trekha.user.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "kyc_documents")
@Data
@NoArgsConstructor
public class KycDocument {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "user_id", nullable = false)
        private User user;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        private KycDocumentType documentType;

        @Column(nullable = false)
        private String documentUrl; // Store path to S3 or local storage

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        private KycStatus status = KycStatus.PENDING;

        @Column(nullable = false, updatable = false)
        private LocalDateTime uploadedAt;

        private LocalDateTime reviewedAt;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "reviewer_id") // Admin user who reviewed
        private User reviewer; // This implies an Admin role/user who can review

        private String rejectionReason;

    // Consider adding expiryDate for documents like licenses
    private LocalDate expiryDate;
}
