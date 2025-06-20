package com.app.trekha.user.controller;

import com.app.trekha.TrekhaApplication;
import com.app.trekha.user.dto.LoginRequest;
import com.app.trekha.user.model.ERole;
import com.app.trekha.user.model.RegistrationMethod;
import com.app.trekha.user.model.Role;
import com.app.trekha.user.model.User;
import com.app.trekha.user.repository.RoleRepository;
import com.app.trekha.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = TrekhaApplication.class) // Explicitly specify the main application class
@AutoConfigureMockMvc
@Transactional // Rollback changes after each test
@ActiveProfiles("test") // Use a test profile if you have one for in-memory DB etc.
public class AuthControllerLoginTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private final String TEST_EMAIL = "testuser@example.com";
    private final String TEST_MOBILE = "+1234567890";
    private final String TEST_PASSWORD = "password123";

    @BeforeEach
    void setUp() {
        // Ensure the PASSENGER role exists
        Role passengerRole = roleRepository.findByName(ERole.ROLE_PASSENGER)
                .orElseGet(() -> roleRepository.save(new Role(ERole.ROLE_PASSENGER)));

        // Create a test user
        testUser = new User();
        testUser.setEmail(TEST_EMAIL);
        testUser.setMobileNumber(TEST_MOBILE);
        testUser.setPasswordHash(passwordEncoder.encode(TEST_PASSWORD));
        testUser.setRegistrationMethod(RegistrationMethod.EMAIL); // Or MOBILE, doesn't matter for login
        testUser.setActive(true);
        testUser.setEmailVerified(true); // Assume verified for login test
        testUser.setMobileVerified(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());

        Set<Role> roles = new HashSet<>();
        roles.add(passengerRole);
        testUser.setRoles(roles);

        userRepository.save(testUser);
    }

    @Test
    void shouldAuthenticateUserWithEmailAndReturnJwt() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setLoginIdentifier(TEST_EMAIL);
        loginRequest.setPassword(TEST_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.id").value(testUser.getId()))
                .andExpect(jsonPath("$.identifier").value(TEST_EMAIL))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.roles[0]").value(ERole.ROLE_PASSENGER.name()));
    }

    @Test
    void shouldAuthenticateUserWithMobileAndReturnJwt() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setLoginIdentifier(TEST_MOBILE);
        loginRequest.setPassword(TEST_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty()); // Basic check
    }

    @Test
    void shouldReturnUnauthorizedForInvalidCredentials() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setLoginIdentifier(TEST_EMAIL);
        loginRequest.setPassword("wrongpassword");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized()); // Spring Security returns 401 for bad credentials
    }

    @Test
    void shouldReturnUnauthorizedForNonExistentUser() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setLoginIdentifier("nonexistent@example.com");
        loginRequest.setPassword(TEST_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnBadRequestForMissingIdentifier() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setPassword(TEST_PASSWORD);
        // loginIdentifier is missing

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest()); // Handled by @Valid and GlobalExceptionHandler
    }

    @Test
    void shouldReturnBadRequestForMissingPassword() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setLoginIdentifier(TEST_EMAIL);
        // password is missing

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest()); // Handled by @Valid and GlobalExceptionHandler
    }
}
