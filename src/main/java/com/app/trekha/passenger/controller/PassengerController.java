package com.app.trekha.passenger.controller;

import com.app.trekha.passenger.dto.PassengerProfileUpdateRequest;
import com.app.trekha.passenger.service.PassengerService;
import com.app.trekha.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/passengers")
@RequiredArgsConstructor
public class PassengerController {

    private final PassengerService passengerService;

    @PutMapping("/me/profile")
    @PreAuthorize("hasRole('PASSENGER') and #userId == authentication.principal.id") // Ensure user can only update their own profile
    public ResponseEntity<UserResponse> updateMyProfile(
            @PathVariable Long userId, // Get user ID from path for @PreAuthorize check
            @Valid @ModelAttribute PassengerProfileUpdateRequest request) { // Use @ModelAttribute for multipart/form-data

        // Get the authenticated user's ID from the security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        // Assuming your UserDetails implementation provides the User ID
        Long authenticatedUserId = ((com.app.trekha.user.model.User) userDetails).getId(); // Cast to your User entity

        UserResponse updatedProfile = passengerService.updatePassengerProfile(authenticatedUserId, request);
        return ResponseEntity.ok(updatedProfile);
    }
}
