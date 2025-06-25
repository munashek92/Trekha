package com.app.trekha.user.dto;

import com.app.trekha.user.model.KycDocumentType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Data
public class KycUploadRequest {

    @NotNull(message = "Document type cannot be null")
    private KycDocumentType documentType;

    @NotNull(message = "File cannot be null")
    private MultipartFile file;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expiryDate; // Optional, for documents like licenses
}

