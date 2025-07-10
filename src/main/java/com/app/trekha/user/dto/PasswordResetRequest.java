package com.app.trekha.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordResetRequest {
    @NotBlank(message = "New password cannot be blank")
    private String newPassword;

    // You might add a "confirm password" field here for better UX.
}