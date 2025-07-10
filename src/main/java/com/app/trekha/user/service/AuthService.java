package com.app.trekha.user.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;import com.app.trekha.user.dto.PasswordResetRequest;

import com.app.trekha.common.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;

import com.app.trekha.common.service.EmailService;
import com.app.trekha.common.service.SmsService;
import com.app.trekha.config.security.JwtService;
import com.app.trekha.user.dto.JwtResponse;
import com.app.trekha.user.dto.LoginRequest;
import com.app.trekha.user.dto.MobileVerificationRequest;
import com.app.trekha.user.dto.PassengerRegistrationRequest;
import com.app.trekha.user.dto.UserResponse;
import com.app.trekha.user.model.ERole;
import com.app.trekha.user.model.PassengerProfile;
import com.app.trekha.user.model.RegistrationMethod;
import com.app.trekha.user.model.Role;
import com.app.trekha.user.model.User;
import com.app.trekha.user.model.VerificationToken;
import com.app.trekha.user.repository.PassengerProfileRepository;
import com.app.trekha.user.repository.PasswordResetTokenRepository;
import com.app.trekha.user.repository.RoleRepository;
import com.app.trekha.user.repository.UserRepository;
import com.app.trekha.user.repository.VerificationTokenRepository;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
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
    private final VerificationTokenRepository tokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final SmsService smsService;

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
        // Set verification status - initially false for email/mobile requiring verification
        if (method == RegistrationMethod.EMAIL) {
            user.setEmailVerified(false); // Will require email verification
        } else if (method == RegistrationMethod.MOBILE) {
            user.setMobileVerified(false); // Will require mobile OTP verification
        }

        User savedUser = userRepository.save(user);

        PassengerProfile passengerProfile = getPassengerProfile(request, savedUser);

        passengerProfileRepository.save(passengerProfile);

        createAndSendVerificationToken(savedUser, passengerProfile, method);


        return mapToUserResponse(savedUser, passengerProfile);
    }

    private void createAndSendVerificationToken(User user, PassengerProfile profile, RegistrationMethod method) {
        // Invalidate any existing token for this user
        tokenRepository.findByUser(user).ifPresent(tokenRepository::delete);

        if (method == RegistrationMethod.EMAIL) {
            String token = UUID.randomUUID().toString();
            VerificationToken verificationToken = new VerificationToken(token, user);
            tokenRepository.save(verificationToken);

            String verificationUrl = "http://localhost:8080/api/v1/auth/verify-email?token=" + token; // In prod, get base URL from config
            String emailBody = "Hi " + profile.getFirstName() + ",\n\n"
                    + "Please click the link below to verify your email address:\n"
                    + verificationUrl + "\n\n"
                    + "This link will expire in 15 minutes.\n\n"
                    + "Thanks,\nThe Trekha Team";
            emailService.sendEmail(user.getEmail(), "Trekha - Verify Your Email", emailBody);

        } else if (method == RegistrationMethod.MOBILE) {
            // Generate a 6-digit OTP
            String otp = String.format("%06d", new Random().nextInt(999999));
            VerificationToken verificationToken = new VerificationToken(otp, user);
            tokenRepository.save(verificationToken);

            String smsBody = "Your Trekha verification code is: " + otp + ". It will expire in 15 minutes.";
            smsService.sendSms(user.getMobileNumber(), smsBody);
        }
    }

    @Transactional
    public void verifyEmail(String token) {
        VerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification token."));

        if (verificationToken.isExpired()) {
            tokenRepository.delete(verificationToken);
            throw new IllegalArgumentException("Verification token has expired.");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        // Token has been used, so we can delete it.
        tokenRepository.delete(verificationToken);
    }

    @Transactional
    public void verifyMobile(String mobileNumber, String otp) {
        User user = userRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new ResourceNotFoundException("User", "mobileNumber", mobileNumber));

        VerificationToken verificationToken = tokenRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("No verification token found for this user. Please request a new one."));

        if (verificationToken.isExpired()) {
            tokenRepository.delete(verificationToken);
            throw new IllegalArgumentException("Verification token has expired. Please request a new one.");
        }

        if (!verificationToken.getToken().equals(otp)) {
            throw new IllegalArgumentException("Invalid verification code.");
        }

        user.setMobileVerified(true);
        userRepository.save(user);

        // Token has been used, so we can delete it.
        tokenRepository.delete(verificationToken);
    }
    
    @Transactional
    public void requestPasswordReset(String loginIdentifier) {
        User user = userRepository.findByEmail(loginIdentifier)
            .orElseGet(() -> userRepository.findByMobileNumber(loginIdentifier)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with identifier: " + loginIdentifier)));

        // Check if the account is active
        if (!user.isActive()) {
            throw new IllegalStateException("Account is not active. Please contact support.");
        }

        // Check verification status based on the registration method
        if (user.getRegistrationMethod() == RegistrationMethod.EMAIL && !user.isEmailVerified()) {
            throw new IllegalStateException("Email is not verified. Please verify your email first.");
        } else if (user.getRegistrationMethod() == RegistrationMethod.MOBILE && !user.isMobileVerified()) {
            throw new IllegalStateException("Mobile number is not verified. Please verify your mobile number first.");
        }

        // Invalidate any existing token for this user
        passwordResetTokenRepository.findByUser(user).ifPresent(passwordResetTokenRepository::delete);

        // Generate a reset token
        String token = UUID.randomUUID().toString();
        com.app.trekha.user.model.PasswordResetToken resetToken = new com.app.trekha.user.model.PasswordResetToken(token, user);
        passwordResetTokenRepository.save(resetToken);

        // Send reset email
        String resetUrl = "http://localhost:8080/api/v1/auth/reset-password?token=" + token; // In prod, get base URL from config
        String emailBody = "Hi " + (user.getUsername() != null ? user.getUsername() : "User") + ",\n\n"
            + "You requested a password reset. Please click the link below to reset your password:\n"
            + resetUrl + "\n\n"
            + "This link will expire in 30 minutes.\n\n"
            + "If you did not request a password reset, please ignore this email.\n\n"
            + "Thanks,\nThe Trekha Team";
        emailService.sendEmail(user.getEmail(), "Trekha - Reset Your Password", emailBody);
    }

    @Transactional
    public void resetPassword(String token, PasswordResetRequest request) {
        com.app.trekha.user.model.PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
            .orElseThrow(() -> new IllegalArgumentException("Invalid or expired password reset token. Please request a new one."));

        if (resetToken.isExpired()) {
            passwordResetTokenRepository.delete(resetToken);
            throw new IllegalArgumentException("Password reset token has expired. Please request a new one.");
        }

        if (!isValidPassword(request.getNewPassword())) {
            throw new IllegalArgumentException("Invalid password. Password must be at least 8 characters long and contain at least one digit.");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Token has been used, so we can delete it.
        passwordResetTokenRepository.delete(resetToken);
    }

    private boolean isValidPassword(String password) {
        // Add your password validation logic here (e.g., length, special characters, etc.)
        return password != null && password.length() >= 8 && password.matches(".*\\d.*");
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

