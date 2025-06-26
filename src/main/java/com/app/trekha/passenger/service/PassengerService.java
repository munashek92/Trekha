package com.app.trekha.passenger.service;

import com.app.trekha.common.exception.ResourceNotFoundException;
import com.app.trekha.passenger.dto.PassengerProfileUpdateRequest;
import com.app.trekha.storage.FileStorageService;
import com.app.trekha.user.dto.UserResponse;
import com.app.trekha.user.model.PassengerProfile;
import com.app.trekha.user.model.User;
import com.app.trekha.user.repository.PassengerProfileRepository;
import com.app.trekha.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PassengerService {

    private final PassengerProfileRepository passengerProfileRepository;
    private final UserRepository userRepository; // Needed to map User to UserResponse
    private final FileStorageService fileStorageService; // Assuming you have this service

    @Transactional
    public UserResponse updatePassengerProfile(Long userId, PassengerProfileUpdateRequest request) {
        // Find the user and their profile
        if (userId == null || request == null) {
            throw new NullPointerException("User ID and update request must not be null");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        PassengerProfile profile = passengerProfileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("PassengerProfile", "userId", userId));

        // Update profile fields
        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());

        // Handle profile picture upload if present
        MultipartFile profilePicture = request.getProfilePicture();
        if (profilePicture != null && !profilePicture.isEmpty()) {
            try {
                // Store the file and get its URL/path
                String fileUrl = fileStorageService.store(profilePicture, "profile-pictures/" + userId);
                profile.setProfilePictureUrl(fileUrl);
            } catch (IOException e) {
                // Handle file storage exception (e.g., log it, throw a custom exception)
                throw new RuntimeException("Failed to store profile picture", e);
            }
        }

        PassengerProfile updatedProfile = passengerProfileRepository.save(profile);

        // Return the updated user/profile details
        return mapToUserResponse(user, updatedProfile);
    }

    // Helper method to map User and PassengerProfile to UserResponse (can be shared or in a mapper class)
    private UserResponse mapToUserResponse(User user, PassengerProfile profile) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setMobileNumber(user.getMobileNumber());
        response.setFirstName(profile.getFirstName());
        response.setLastName(profile.getLastName());
        response.setProfilePictureUrl(profile.getProfilePictureUrl());
        response.setActive(user.isActive());
        response.setEmailVerified(user.isEmailVerified());
        response.setMobileVerified(user.isMobileVerified());
        response.setOnboardingCompleted(profile.isOnboardingCompleted());


        Set<String> roleNames = new java.util.HashSet<>();
        if (user.getRoles() != null) {
            user.getRoles().forEach(role -> roleNames.add(role.getName().name()));
        }
        response.setRoles(roleNames);
        return response;
    }

    @Transactional
    public UserResponse completeOnboarding(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        PassengerProfile profile = passengerProfileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("PassengerProfile", "userId", userId));

        profile.setOnboardingCompleted(true);
        PassengerProfile updatedProfile = passengerProfileRepository.save(profile);

        return mapToUserResponse(user, updatedProfile);
    }

    public UserResponse getPassengerProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        PassengerProfile profile = passengerProfileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("PassengerProfile", "userId", userId));

        return mapToUserResponse(user, profile);
    }

}
