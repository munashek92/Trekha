package com.app.trekha.user.controller;

import com.app.trekha.user.dto.PassengerRegistrationRequest;
import com.app.trekha.user.dto.UserResponse;
import com.app.trekha.user.model.RegistrationMethod;
import com.app.trekha.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    // TODO: Add /login endpoint
    // TODO: Add social login endpoints (/google, /facebook, /apple)
}
