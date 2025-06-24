package com.app.trekha;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAllowPublicAccessToAuthEndpoints() throws Exception {
        // Assuming you have a GET endpoint for demonstration, like /api/v1/auth/some-public-info
        // For POST endpoints like /login or /register, you'd use post()
        // This test verifies that a public endpoint is indeed public.
        mockMvc.perform(get("/api/v1/auth/register/passenger/email"))
                .andExpect(status().isOk()); // Or whatever the expected status is
    }

    @Test
    void shouldDenyAccessToProtectedEndpointWithoutAuthentication() throws Exception {
        // This test verifies that a protected endpoint is secured
        // and that your custom AuthenticationEntryPoint is working.
        mockMvc.perform(get("/api/v1/passengers/me/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.details").value("uri=/api/v1/some-protected-endpoint"));
    }

    @Test
    @WithMockUser // Simulates an authenticated user for this test
    void shouldAllowAccessToProtectedEndpointWithAuthentication() throws Exception {
        // This test verifies that a user who is authenticated can access the endpoint.
        // Note: This doesn't test your JWT logic, but the general security rule.
        // To test with a real JWT, you'd generate one and add it as a header.
        mockMvc.perform(get("/api/v1/passengers/me/profile"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"PASSENGER"}) // Simulates user with a specific role
    void shouldAllowAccessForCorrectRole() throws Exception {
        // Assuming an endpoint is secured with @PreAuthorize("hasRole('PASSENGER')")
        mockMvc.perform(put("/api/v1/passengers/me/profile"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"DRIVER"}) // Simulates user with the wrong role
    void shouldDenyAccessForIncorrectRole() throws Exception {
        // Assuming an endpoint is secured with @PreAuthorize("hasRole('PASSENGER')")
        mockMvc.perform(get("/api/v1/passengers/me/profile"))
                .andExpect(status().isForbidden()); // 403 Forbidden for role issues
    }
}

