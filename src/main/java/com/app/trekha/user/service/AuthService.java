package com.app.trekha.user.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.springframework.security.core.Authentication;
import com.app.trekha.config.security.JwtService;
import com.app.trekha.user.dto.JwtResponse;
import com.app.trekha.user.dto.LoginRequest;
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

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PassengerProfileRepository passengerProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public UserResponse registerPassenger(PassengerRegistrationRequest request, RegistrationMethod method) {
        validateRequest(request, method);

        User user = getUser(request, method);

        // if(method == RegistrationMethod.EMAIL){
        //     user.setEmailVerified (true); // Send verification email
        // } else if(method == RegistrationMethod.MOBILE){
        //     user.setMobileVerified(true); // Send OTP
        // }

        Set<Role> roles = new HashSet<>();
        Role passengerRole = roleRepository.findByName(ERole.ROLE_PASSENGER)
                .orElseThrow(() -> new RuntimeException("Error: Role PASSENGER is not found."));
        roles.add(passengerRole);
        user.setRoles(roles);

        // For now, mark as active. Email/Mobile verification can be added later.
        user.setActive(true);
        // Set verification status - initially false for email/mobile requiring verification
        if (method == RegistrationMethod.EMAIL) {
            user.setEmailVerified(false); // Will require email verification
        } else if (method == RegistrationMethod.MOBILE) {
            user.setMobileVerified(false); // Will require mobile OTP verification
        }

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
        passengerProfile.setCreatedAt(LocalDateTime.now());
        passengerProfile.setUpdatedAt(LocalDateTime.now()); // Explicitly set updatedAt on creation
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
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now()); // Explicitly set updatedAt on creation

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

        // Set verification status from the User entity
        response.setEmailVerified(user.isEmailVerified());
        response.setMobileVerified(user.isMobileVerified());
        response.setActive(user.isActive());
    
    
        if (profile != null) {
            response.setFirstName(profile.getFirstName());
            response.setLastName(profile.getLastName());
            response.setProfilePictureUrl(profile.getProfilePictureUrl());
            response.setOnboardingCompleted(profile.isOnboardingCompleted()); // Add onboarding status

        }
        Set<String> roleNames = new HashSet<>();
        if (user.getRoles() != null) {
            user.getRoles().forEach(role -> roleNames.add(role.getName().name()));
        }
        response.setRoles(roleNames);
        return response;
    }

    public JwtResponse loginUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getLoginIdentifier(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // The principal is your custom User entity which implements UserDetails
        com.app.trekha.user.model.User authenticatedUserPrincipal =
            (com.app.trekha.user.model.User) authentication.getPrincipal();


        String jwt = jwtService.generateToken(authenticatedUserPrincipal);

        User userEntity = userRepository.findByEmail(authenticatedUserPrincipal.getUsername())
                .orElseGet(() -> userRepository.findByMobileNumber(authenticatedUserPrincipal.getUsername())
                        .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException("User not found after successful authentication with: " + authenticatedUserPrincipal.getUsername())));

        Set<String> roles = authenticatedUserPrincipal.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toSet());

        // Update last login time
        userEntity.setLastLoginAt(java.time.LocalDateTime.now());
        userRepository.save(userEntity);

        return new JwtResponse(jwt, userEntity.getId(), authenticatedUserPrincipal.getUsername(), roles);
    }

}
