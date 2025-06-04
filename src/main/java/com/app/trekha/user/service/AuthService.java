package com.app.trekha.user.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.app.trekha.user.dto.PassengerRegistrationRequest;
import com.app.trekha.user.dto.UserResponse;
import com.app.trekha.user.model.ERole;
import com.app.trekha.user.model.PassengerProfile;
import com.app.trekha.user.model.RegistrationMethod;
import com.app.trekha.user.model.Role;
import com.app.trekha.user.model.User;
import com.app.trekha.user.repository.PassengerProfileRepository;
import com.app.trekha.user.repository.RoleRepository;
import com.app.trekha.user.repository.UserRepository;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PassengerProfileRepository passengerProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse registerPassenger(PassengerRegistrationRequest request, RegistrationMethod method) {
        validateRequest(request, method);

        User user = getUser(request, method);

        Set<Role> roles = new HashSet<>();
        Role passengerRole = roleRepository.findByName(ERole.ROLE_PASSENGER)
                .orElseThrow(() -> new RuntimeException("Error: Role PASSENGER is not found."));
        roles.add(passengerRole);
        user.setRoles(roles);

        // For now, mark as active. Email/Mobile verification can be added later.
        user.setActive(true);
        if (method == RegistrationMethod.EMAIL) user.setEmailVerified(false); // Send verification email
        if (method == RegistrationMethod.MOBILE) user.setMobileVerified(false); // Send OTP

        User savedUser = userRepository.save(user);

        PassengerProfile passengerProfile = getPassengerProfile(request, savedUser);

        passengerProfileRepository.save(passengerProfile);

        return mapToUserResponse(savedUser, passengerProfile);
    }

    private PassengerProfile getPassengerProfile(PassengerRegistrationRequest request, User savedUser) {
        // Create Passenger Profile
        PassengerProfile passengerProfile = new PassengerProfile();
        passengerProfile.setUser(savedUser); // This sets the userId as well due to @MapsId
        passengerProfile.setFirstName(request.getFirstName());
        passengerProfile.setLastName(request.getLastName());
        // passengerProfile.setProfilePictureUrl(); // Can be updated later
        return passengerProfile;
    }

    private User getUser(PassengerRegistrationRequest request, RegistrationMethod method) {
        // Create new user's account
        User user = new User();
        user.setEmail(request.getEmail());
        user.setMobileNumber(request.getMobileNumber());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRegistrationMethod(method);
        return user;
    }

    private void validateRequest(PassengerRegistrationRequest request, RegistrationMethod method) {
        if (method == RegistrationMethod.EMAIL) {
            if (request.getEmail() == null || request.getEmail().isEmpty()) {
                throw new IllegalArgumentException("Email is required for email registration.");
            }
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Error: Email is already in use!");
            }
        } else if (method == RegistrationMethod.MOBILE) {
            if (request.getMobileNumber() == null || request.getMobileNumber().isEmpty()) {
                throw new IllegalArgumentException("Mobile number is required for mobile registration.");
            }
            if (userRepository.existsByMobileNumber(request.getMobileNumber())) {
                throw new IllegalArgumentException("Error: Mobile number is already in use!");
            }
        } else {
            throw new IllegalArgumentException("Unsupported registration method for this flow.");
        }
    }

    private UserResponse mapToUserResponse(User user, PassengerProfile profile) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setMobileNumber(user.getMobileNumber());
        if (profile != null) {
            response.setFirstName(profile.getFirstName());
            response.setLastName(profile.getLastName());
            response.setProfilePictureUrl(profile.getProfilePictureUrl());
        }
        Set<String> roleNames = new HashSet<>();
        if (user.getRoles() != null) {
            user.getRoles().forEach(role -> roleNames.add(role.getName().name()));
        }
        response.setRoles(roleNames);
        return response;
    }

    // TODO: Implement login, social login methods
}
