package com.app.trekha.config.security;

import static org.junit.jupiter.api.Assertions.*;

import java.security.Key;
import java.util.Collections;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

class JwtServiceTest {

    private static final String TEST_SECRET_KEY = "dGhpcy1pcy1hLXZlcnktbG9uZy1hbmQtc2VjdXJlLXNlY3JldC1rZXktZm9yLXNwcmluZy1ib290LXRlc3Rpbmc=";

    private JwtService jwtService;

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", TEST_SECRET_KEY);
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 3600000); // 1 hour

        userDetails = User.withUsername("testuser@gmail.com")
                .password("password")
                .authorities(Collections.singletonList(() -> "ROLE_USER"))
                .build();
    }

    @Test
    void extractUsernameShouldReturnCorrectUsername() {
        String token = jwtService.generateToken(userDetails);
        String extractedUsername = jwtService.extractUsername(token);
        assertEquals(userDetails.getUsername(), extractedUsername);
    }

    @Test
    void generateTokenShouldCreateValidToken() {
        String token = jwtService.generateToken(userDetails);
        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    void isTokenValidWithValidTokenShouldReturnTrue() {
        String token = jwtService.generateToken(userDetails);
        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    void isTokenValidWithDifferentUserShouldReturnFalse() {
        String token = jwtService.generateToken(userDetails);
        UserDetails otherUserDetails = User.withUsername("otheruser@example.com")
                .password("password")
                .authorities(Collections.emptyList())
                .build();

        assertFalse(jwtService.isTokenValid(token, otherUserDetails));
    }

    @Test
    void isTokenValidWithExpiredTokenShouldReturnFalse() {
        // Generate a token that expired 1 second ago
        Date expiration = new Date(System.currentTimeMillis() - 1000);
        String expiredToken = generateTokenWithExpiration(userDetails, expiration);

        assertFalse(jwtService.isTokenValid(expiredToken, userDetails));
    }

    @Test
    void extractClaimOnExpiredTokenShouldThrowExpiredJwtException() {
        // Generate a token that expired 1 second ago
        Date expiration = new Date(System.currentTimeMillis() - 1000);
        String expiredToken = generateTokenWithExpiration(userDetails, expiration);

        assertThrows(ExpiredJwtException.class, () -> {
            jwtService.extractUsername(expiredToken);
        });

    }

    @Test
    void isTokenValidWithMalformedTokenShouldThrowException() {
        String malformedToken = "this.is.not.a.valid.jwt";
        assertThrows(MalformedJwtException.class, () -> {
            jwtService.isTokenValid(malformedToken, userDetails);
        });
    }

    @Test
    void isTokenValidWithInvalidSignatureShouldThrowException() {
        // Generate a token with the original service
        String token = jwtService.generateToken(userDetails);

        // Create another service with a different secret key
        JwtService otherJwtService = new JwtService();
        String anotherSecretKey = "YW5vdGhlci12ZXJ5LWxvbmctYW5kLXNlY3VyZS1zZWNyZXQta2V5LWZvci10ZXN0aW5nLXB1cnBvc2Vz";
        ReflectionTestUtils.setField(otherJwtService, "jwtSecret", anotherSecretKey);
        ReflectionTestUtils.setField(otherJwtService, "jwtExpirationMs", 3600000);

        // Try to validate the token with the wrong service (and thus wrong key)
        assertThrows(SignatureException.class, () -> {
            otherJwtService.isTokenValid(token, userDetails);
        });
    }

    @Test
    void generateTokenWithNullUserDetailsShouldThrowNullPointerException() {
        assertThrows(NullPointerException.class, () -> {
            jwtService.generateToken(null);
        }, "generateToken should throw NullPointerException when userDetails is null");
    }

    @Test
    void isTokenValidWithNullUserDetailsShouldThrowNullPointerException() {
        // Generate a valid token first
        String token = jwtService.generateToken(userDetails);
        assertThrows(NullPointerException.class, () -> {
            jwtService.isTokenValid(token, null);
        }, "isTokenValid should throw NullPointerException when userDetails is null");
    }

    private String generateTokenWithExpiration(UserDetails userDetails, Date expiration) {
        byte[] keyBytes = Decoders.BASE64.decode(TEST_SECRET_KEY);
        Key signingKey = Keys.hmacShaKeyFor(keyBytes);

        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(expiration)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }
}
