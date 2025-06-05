package com.app.trekha.user.dto;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String mobileNumber;
    private Set<String> roles;
    private String profilePictureUrl; // From PassengerProfile
    private boolean isEmailVerified = false;
    private boolean isMobileVerified = false;
    private boolean isActive = true;
}
