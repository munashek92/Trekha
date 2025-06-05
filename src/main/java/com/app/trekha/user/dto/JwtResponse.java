package com.app.trekha.user.dto;

import java.util.Set;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private Long id;
    private String identifier; // email or mobile used for login
    private Set<String> roles;

    public JwtResponse(String accessToken, Long id, String identifier, Set<String> roles) {
        this.token = accessToken;
        this.id = id;
        this.identifier = identifier;
        this.roles = roles;
    }
}
