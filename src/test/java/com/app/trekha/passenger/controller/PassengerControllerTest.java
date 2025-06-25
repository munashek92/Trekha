package com.app.trekha.passenger.controller;

import com.app.trekha.config.security.JwtService;
import com.app.trekha.config.security.WithMockCustomUser;
import com.app.trekha.passenger.service.PassengerService;
import com.app.trekha.user.dto.KycDocumentResponse;
import com.app.trekha.user.dto.UserResponse;
import com.app.trekha.user.model.KycDocumentType;
import com.app.trekha.user.model.KycStatus;
import com.app.trekha.user.service.KycService;
import com.app.trekha.user.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PassengerController.class)
class PassengerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PassengerService passengerService;

    @MockBean
    private KycService kycService;

    // These are required by Spring Security when it's on the classpath for @WebMvcTest
    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    @WithMockCustomUser
    void getMyProfile_whenAuthenticated_shouldReturnProfile() throws Exception {
        UserResponse userResponse = new UserResponse(1L, "Test", "User", "test@example.com", "1234567890", Set.of("ROLE_PASSENGER"), null, true, true, true, true);
        when(passengerService.getPassengerProfile(1L)).thenReturn(userResponse);

        mockMvc.perform(get("/api/v1/passengers/me/profile"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.firstName", is("Test")))
                .andExpect(jsonPath("$.email", is("test@example.com")));
    }

    @Test
    @WithMockCustomUser
    void updateMyProfile_withValidData_shouldUpdateProfile() throws Exception {
        UserResponse updatedResponse = new UserResponse(1L, "Updated", "User", "test@example.com", "1234567890", Set.of("ROLE_PASSENGER"), null, true, true, true, true);
        when(passengerService.updatePassengerProfile(eq(1L), any())).thenReturn(updatedResponse);

        MockMultipartFile profilePicture = new MockMultipartFile("profilePicture", "test.jpg", "image/jpeg", "test image".getBytes());

        // MockMvc doesn't have a direct multipart PUT, so we use a workaround
        mockMvc.perform(multipart("/api/v1/passengers/me/profile")
                        .file(profilePicture)
                        .param("firstName", "Updated")
                        .param("lastName", "User")
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("Updated")));
    }

    @Test
    @WithMockCustomUser
    void markOnboardingAsComplete_whenAuthenticated_shouldSucceed() throws Exception {
        UserResponse userResponse = new UserResponse();
        userResponse.setOnboardingCompleted(true);
        when(passengerService.completeOnboarding(1L)).thenReturn(userResponse);

        mockMvc.perform(post("/api/v1/passengers/me/complete-onboarding"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onboardingCompleted", is(true)));
    }

    @Test
    @WithMockCustomUser
    void uploadKycDocument_withValidData_shouldSucceed() throws Exception {
        KycDocumentResponse kycResponse = new KycDocumentResponse(1L, 1L, KycDocumentType.NATIONAL_ID, "/kyc-documents/1/file.jpg", KycStatus.PENDING, LocalDateTime.now(), null, null);
        when(kycService.uploadKycDocument(eq(1L), any())).thenReturn(kycResponse);

        MockMultipartFile kycFile = new MockMultipartFile("file", "national_id.jpg", "image/jpeg", "kyc data".getBytes());

        mockMvc.perform(multipart("/api/v1/passengers/me/kyc-upload")
                        .file(kycFile)
                        .param("documentType", "NATIONAL_ID"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.documentType", is("NATIONAL_ID")));
    }

    @Test
    @WithMockCustomUser
    void uploadKycDocument_withMissingFile_shouldReturnBadRequest() throws Exception {
        // The @NotNull validation on the 'file' field in KycUploadRequest will trigger a 400 Bad Request.
        // We are not sending the 'file' part in this request.
        mockMvc.perform(multipart("/api/v1/passengers/me/kyc-upload")
                        .param("documentType", "NATIONAL_ID"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockCustomUser
    void uploadKycDocument_withMissingDocumentType_shouldReturnBadRequest() throws Exception {
        // The @NotNull validation on the 'documentType' field will trigger a 400 Bad Request.
        MockMultipartFile kycFile = new MockMultipartFile("file", "national_id.jpg", "image/jpeg", "kyc data".getBytes());

        mockMvc.perform(multipart("/api/v1/passengers/me/kyc-upload")
                        .file(kycFile))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMyProfile_whenUnauthenticated_shouldReturnUnauthorized() throws Exception {
        // No @WithMockCustomUser, so the request is anonymous.
        // Spring Security's filter chain should deny access.
        mockMvc.perform(get("/api/v1/passengers/me/profile"))
                .andExpect(status().isUnauthorized());
    }
}