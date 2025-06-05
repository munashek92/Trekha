package com.app.trekha.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Login identifier (email or mobile) is required")
    private String loginIdentifier; // Can be email or mobile number

    @NotBlank(message = "Password is required")
    private String password;
}
