package com.app.trekha.passenger.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.trekha.passenger.dto.PassengerProfileUpdateRequest;
import com.app.trekha.passenger.service.PassengerService;
import com.app.trekha.user.dto.KycDocumentResponse;
import com.app.trekha.user.dto.UserResponse;
import com.app.trekha.user.service.KycService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/passengers")
@RequiredArgsConstructor
public class PassengerController {

    private final PassengerService passengerService;
    private final KycService kycService;


    @PutMapping("/me/profile")
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<UserResponse> updateMyProfile(
            @Valid @ModelAttribute PassengerProfileUpdateRequest request) { // Use @ModelAttribute for multipart/form-data

        // Get the authenticated user's ID from the security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long authenticatedUserId = ((com.app.trekha.user.model.User) authentication.getPrincipal()).getId();

        UserResponse updatedProfile = passengerService.updatePassengerProfile(authenticatedUserId, request);
        return ResponseEntity.ok(updatedProfile);
    }

    @PostMapping("/me/complete-onboarding")
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<UserResponse> markOnboardingAsComplete() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // Assuming your UserDetails implementation (your User entity) provides the User ID
        Long authenticatedUserId = ((com.app.trekha.user.model.User) authentication.getPrincipal()).getId();

        UserResponse updatedProfile = passengerService.completeOnboarding(authenticatedUserId);
        return ResponseEntity.ok(updatedProfile);
    }
    @GetMapping("/me/profile")
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<UserResponse> getMyProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // Assuming your UserDetails implementation (your User entity) provides the User ID
        Long authenticatedUserId = ((com.app.trekha.user.model.User) authentication.getPrincipal()).getId();

        UserResponse userProfile = passengerService.getPassengerProfile(authenticatedUserId);
        return ResponseEntity.ok(userProfile);
    }

    @PostMapping("/me/kyc-upload")
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<KycDocumentResponse> uploadKycDocument(
            @Valid @ModelAttribute com.app.trekha.user.dto.KycUploadRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long authenticatedUserId = ((com.app.trekha.user.model.User) authentication.getPrincipal()).getId();

        KycDocumentResponse response = kycService.uploadKycDocument(authenticatedUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
