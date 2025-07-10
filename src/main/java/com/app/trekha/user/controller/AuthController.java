package com.app.trekha.user.controller;

import com.app.trekha.user.dto.JwtResponse;
import com.app.trekha.user.dto.LoginRequest;
import com.app.trekha.user.dto.PasswordResetRequest;
import com.app.trekha.user.model.RegistrationMethod;
import com.app.trekha.user.dto.MobileVerificationRequest;
import com.app.trekha.user.dto.*;
import com.app.trekha.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register/passenger/email")
    public ResponseEntity<?> registerPassengerByEmail(@Valid @RequestBody PassengerRegistrationRequest registrationRequest) {
    if (registrationRequest.getEmail() == null || registrationRequest.getEmail().isEmpty()) {
    return ResponseEntity.badRequest().body("Email is required for this registration type.");
    }
    UserResponse userResponse = authService.registerPassenger(registrationRequest, RegistrationMethod.EMAIL);
    return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
    }

@PostMapping("/register/passenger/mobile")
public ResponseEntity<?> registerPassengerByMobile(@Valid @RequestBody PassengerRegistrationRequest registrationRequest) {
    if (registrationRequest.getMobileNumber() == null || registrationRequest.getMobileNumber().isEmpty()) {
        return ResponseEntity.badRequest().body("Mobile number is required for this registration type.");
    }
    UserResponse userResponse = authService.registerPassenger(registrationRequest, RegistrationMethod.MOBILE);
    return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
}
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        JwtResponse jwtResponse = authService.loginUser(loginRequest);
        return ResponseEntity.ok(jwtResponse);
    }

    @GetMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam("token") String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok("Email verified successfully!");
    }

    @PostMapping("/verify-mobile")
    public ResponseEntity<String> verifyMobile(@Valid @RequestBody MobileVerificationRequest request) {
        authService.verifyMobile(request.getMobileNumber(), request.getOtp());
        return ResponseEntity.ok("Mobile number verified successfully!");
    }
    
    @PostMapping("/forgot-password")
    public ResponseEntity<String> requestPasswordReset(@RequestParam("loginIdentifier") String loginIdentifier) {
    authService.requestPasswordReset(loginIdentifier);
    return ResponseEntity.ok("If an account with that identifier exists, a password reset email has been sent.");
    }

@PostMapping("/reset-password")
public ResponseEntity<String> resetPassword(@RequestParam("token") String token,
                                            @Valid @RequestBody PasswordResetRequest request) {
    authService.resetPassword(token, request);
    return ResponseEntity.ok("Password reset successfully!");
}

    // TODO: Add social login endpoints (/google, /facebook, /apple)
}
