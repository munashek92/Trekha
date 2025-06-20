package com.app.trekha.config.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.app.trekha.user.service.UserDetailsServiceImpl;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private SecurityContext securityContext;


    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    private UserDetails dummyUserDetails;

    @BeforeEach
    void setUp() {
        // SecurityContextHolder.clearContext() is called in tearDown
        // to ensure a clean state for each test.
        dummyUserDetails = User.withUsername("testuser@example.com")
                .password("password")
                .authorities(Collections.singletonList(() -> "ROLE_USER"))
                .build();
        // Mock the SecurityContextHolder to return our mocked SecurityContext
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_ValidToken_ShouldSetAuthentication() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer validtoken123");
        when(jwtService.extractUsername("validtoken123")).thenReturn("testuser@example.com");
        when(securityContext.getAuthentication()).thenReturn(null); // No existing auth
        when(userDetailsService.loadUserByUsername("testuser@example.com")).thenReturn(dummyUserDetails);
        when(jwtService.isTokenValid("validtoken123", dummyUserDetails)).thenReturn(true);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        ArgumentCaptor<UsernamePasswordAuthenticationToken> authenticationCaptor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(securityContext).setAuthentication(authenticationCaptor.capture());
        
        assertNotNull(authenticationCaptor.getValue());
        assertEquals("testuser@example.com", authenticationCaptor.getValue().getName());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_NoAuthorizationHeader_ShouldNotSetAuthentication() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(securityContext, never()).setAuthentication(any());
        assertNull(SecurityContextHolder.getContext().getAuthentication()); // Check via getter if context was not mocked to set
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_AuthHeaderNotStartingWithBearer_ShouldNotSetAuthentication() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("InvalidTokenFormat");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_JwtServiceExtractsNullUsername_ShouldNotSetAuthentication() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer tokenWithNullUser");
        when(jwtService.extractUsername("tokenWithNullUser")).thenReturn(null);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_AuthenticationAlreadyPresent_ShouldNotAttemptToReAuthenticate() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer validtoken123");
        when(jwtService.extractUsername("validtoken123")).thenReturn("testuser@example.com");
        // Simulate existing authentication
        UsernamePasswordAuthenticationToken existingAuth = new UsernamePasswordAuthenticationToken("existingUser", null, Collections.emptyList());
        when(securityContext.getAuthentication()).thenReturn(existingAuth);


        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        // Verifies that loadUserByUsername and isTokenValid are not called,
        // and setAuthentication is not called again with a new token.
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(jwtService, never()).isTokenValid(anyString(), any(UserDetails.class));
        verify(securityContext, never()).setAuthentication(argThat(auth -> auth != existingAuth)); // Ensure it wasn't set again
        verify(filterChain).doFilter(request, response);
    }


    @Test
    void doFilterInternal_userDetailsNotFound_shouldNotSetAuthentication() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer tokenForUnknownUser");
        when(jwtService.extractUsername("tokenForUnknownUser")).thenReturn("unknown@example.com");
        when(securityContext.getAuthentication()).thenReturn(null);
        when(userDetailsService.loadUserByUsername("unknown@example.com")).thenThrow(new UsernameNotFoundException("User not found"));

        assertThrows(UsernameNotFoundException.class, () ->
            jwtAuthFilter.doFilterInternal(request, response, filterChain));

        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain, never()).doFilter(request, response); // filterChain.doFilter is not called if exception occurs before it
    }

    @Test
    void doFilterInternal_JwtServiceReturnsInvalidToken_ShouldNotSetAuthentication() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalidtoken123");
        when(jwtService.extractUsername("invalidtoken123")).thenReturn("testuser@example.com");
        when(securityContext.getAuthentication()).thenReturn(null);
        when(userDetailsService.loadUserByUsername("testuser@example.com")).thenReturn(dummyUserDetails);
        when(jwtService.isTokenValid("invalidtoken123", dummyUserDetails)).thenReturn(false);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_UserDetailsServiceReturnsNullUserDetails_ShouldThrowNullPointerExceptionDuringTokenValidation() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer tokenCausesNPE");
        when(jwtService.extractUsername("tokenCausesNPE")).thenReturn("testuser@example.com");
        when(securityContext.getAuthentication()).thenReturn(null);
        // Mock UserDetailsService to return null, which is bad practice for the service but tests filter's resilience
        when(userDetailsService.loadUserByUsername("testuser@example.com")).thenReturn(null);

        when(jwtService.isTokenValid(eq("tokenCausesNPE"), isNull())).thenThrow(new NullPointerException("Simulated NPE from userDetails.getUsername()"));


        assertThrows(NullPointerException.class, () -> {
            jwtAuthFilter.doFilterInternal(request, response, filterChain);
        });

        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain, never()).doFilter(request, response);
    }


    @Test
    void doFilterInternal_JwtServiceExtractUsernameThrowsExpiredJwtException_ShouldPropagateException() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer expiredToken");
        when(jwtService.extractUsername("expiredToken")).thenThrow(new ExpiredJwtException(null, null, "Token expired"));

        assertThrows(ExpiredJwtException.class, () -> {
            jwtAuthFilter.doFilterInternal(request, response, filterChain);
        });

        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain, never()).doFilter(request, response); // Exception occurs before this
    }

    @Test
    void doFilterInternal_JwtServiceExtractUsernameThrowsSignatureException_ShouldPropagateException() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer badSignatureToken");
        // Use the non-deprecated SignatureException from io.jsonwebtoken.security
        when(jwtService.extractUsername("badSignatureToken")).thenThrow(new io.jsonwebtoken.security.SignatureException("JWT signature does not match locally computed signature."));

        assertThrows(io.jsonwebtoken.security.SignatureException.class, () -> {
            jwtAuthFilter.doFilterInternal(request, response, filterChain);
        });
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain, never()).doFilter(request, response);
    }
    @Test
    void doFilterInternal_JwtServiceExtractUsernameThrowsMalformedJwtException_ShouldPropagateException() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer malformedToken");
        when(jwtService.extractUsername("malformedToken")).thenThrow(new MalformedJwtException("JWT strings must contain exactly 2 period characters. Found: 0"));

        assertThrows(MalformedJwtException.class, () -> {
            jwtAuthFilter.doFilterInternal(request, response, filterChain);
        });
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain, never()).doFilter(request, response);
    }
}
