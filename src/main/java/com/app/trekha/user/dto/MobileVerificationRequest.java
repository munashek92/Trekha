package com.app.trekha.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MobileVerificationRequest {
    @NotBlank(message = "Mobile number cannot be blank")
    private String mobileNumber;
    @NotBlank(message = "OTP cannot be blank")
    private String otp;
}