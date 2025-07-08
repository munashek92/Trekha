package com.app.trekha.user.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.app.trekha.common.exception.ResourceNotFoundException;
import com.app.trekha.common.service.EmailService;
import com.app.trekha.common.service.SmsService;
import com.app.trekha.user.dto.PassengerRegistrationRequest;
import com.app.trekha.user.model.ERole;
import com.app.trekha.user.model.PassengerProfile;
import com.app.trekha.user.model.RegistrationMethod;
import com.app.trekha.user.model.Role;
import com.app.trekha.user.model.User;
import com.app.trekha.user.model.VerificationToken;
import com.app.trekha.user.repository.PassengerProfileRepository;
import com.app.trekha.user.repository.RoleRepository;
import com.app.trekha.user.repository.UserRepository;
import com.app.trekha.user.repository.VerificationTokenRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PassengerProfileRepository passengerProfileRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private VerificationTokenRepository tokenRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private SmsService smsService;

    @InjectMocks
    private AuthService authService;

    private PassengerRegistrationRequest emailRequest;
    private PassengerRegistrationRequest mobileRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        emailRequest = new PassengerRegistrationRequest();
        emailRequest.setEmail("test@example.com");
        emailRequest.setPassword("password123");
        emailRequest.setFirstName("Test");
        emailRequest.setLastName("User");

        mobileRequest = new PassengerRegistrationRequest();
        mobileRequest.setMobileNumber("1234567890");
        mobileRequest.setPassword("password123");
        mobileRequest.setFirstName("Test");
        mobileRequest.setLastName("User");

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setMobileNumber("1234567890");
    }

    @Test
    void registerPassenger_WithEmail_ShouldCreateUserAndSendVerificationEmail() {
        // Arrange
        when(userRepository.existsByEmail(emailRequest.getEmail())).thenReturn(false);
        when(roleRepository.findByName(ERole.ROLE_PASSENGER)).thenReturn(Optional.of(new Role(ERole.ROLE_PASSENGER)));
        when(passwordEncoder.encode(emailRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(passengerProfileRepository.save(any(PassengerProfile.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        authService.registerPassenger(emailRequest, RegistrationMethod.EMAIL);

        // Assert
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendEmail(eq("test@example.com"), eq("Trekha - Verify Your Email"), bodyCaptor.capture());
        assertTrue(bodyCaptor.getValue().contains("Hi Test"));
        assertTrue(bodyCaptor.getValue().contains("verify-email?token="));
        verify(smsService, never()).sendSms(any(), any());
        verify(tokenRepository).save(any(VerificationToken.class));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertFalse(userCaptor.getValue().isEmailVerified());
    }

    @Test
    void registerPassenger_WithMobile_ShouldCreateUserAndSendVerificationSms() {
        // Arrange
        when(userRepository.existsByMobileNumber(mobileRequest.getMobileNumber())).thenReturn(false);
        when(roleRepository.findByName(ERole.ROLE_PASSENGER)).thenReturn(Optional.of(new Role(ERole.ROLE_PASSENGER)));
        when(passwordEncoder.encode(mobileRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(passengerProfileRepository.save(any(PassengerProfile.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        authService.registerPassenger(mobileRequest, RegistrationMethod.MOBILE);

        // Assert
        ArgumentCaptor<String> smsBodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(smsService).sendSms(eq("1234567890"), smsBodyCaptor.capture());
        assertTrue(smsBodyCaptor.getValue().startsWith("Your Trekha verification code is: "));
        verify(emailService, never()).sendEmail(any(), any(), any());
        verify(tokenRepository).save(any(VerificationToken.class));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertFalse(userCaptor.getValue().isMobileVerified());
    }

    @Test
    void verifyEmail_WithValidToken_ShouldSetEmailVerifiedToTrue() {
        // Arrange
        VerificationToken token = new VerificationToken("valid-token", testUser);
        when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));

        // Act
        authService.verifyEmail("valid-token");

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertTrue(userCaptor.getValue().isEmailVerified());
        verify(tokenRepository).delete(token);
    }

    @Test
    void verifyEmail_WithInvalidToken_ShouldThrowException() {
        // Arrange
        when(tokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> authService.verifyEmail("invalid-token"));
        assertEquals("Invalid verification token.", exception.getMessage());
    }

    @Test
    void verifyEmail_WithExpiredToken_ShouldThrowException() {
        // Arrange
        VerificationToken expiredToken = new VerificationToken("expired-token", testUser);
        expiredToken.setExpiryDate(LocalDateTime.now().minusMinutes(30)); // Manually expire it
        when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> authService.verifyEmail("expired-token"));
        assertEquals("Verification token has expired.", exception.getMessage());
        verify(tokenRepository).delete(expiredToken);
    }

    @Test
    void verifyMobile_WithValidOtp_ShouldSetMobileVerifiedToTrue() {
        // Arrange
        String otp = "123456";
        VerificationToken token = new VerificationToken(otp, testUser);
        when(userRepository.findByMobileNumber("1234567890")).thenReturn(Optional.of(testUser));
        when(tokenRepository.findByUser(testUser)).thenReturn(Optional.of(token));

        // Act
        authService.verifyMobile("1234567890", otp);

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertTrue(userCaptor.getValue().isMobileVerified());
        verify(tokenRepository).delete(token);
    }

    @Test
    void verifyMobile_WithInvalidOtp_ShouldThrowException() {
        // Arrange
        String correctOtp = "123456";
        String wrongOtp = "999999";
        VerificationToken token = new VerificationToken(correctOtp, testUser);
        when(userRepository.findByMobileNumber("1234567890")).thenReturn(Optional.of(testUser));
        when(tokenRepository.findByUser(testUser)).thenReturn(Optional.of(token));

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> authService.verifyMobile("1234567890", wrongOtp));
        assertEquals("Invalid verification code.", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void verifyMobile_WithNonExistentUser_ShouldThrowException() {
        // Arrange
        when(userRepository.findByMobileNumber("non-existent-number")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> authService.verifyMobile("non-existent-number", "123456"));
    }

    @Test
    void verifyMobile_WithNoTokenForUser_ShouldThrowException() {
        // Arrange
        when(userRepository.findByMobileNumber("1234567890")).thenReturn(Optional.of(testUser));
        when(tokenRepository.findByUser(testUser)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> authService.verifyMobile("1234567890", "123456"));
        assertEquals("No verification token found for this user. Please request a new one.", exception.getMessage());
    }

    @Test
    void verifyMobile_WithExpiredToken_ShouldThrowException() {
        // Arrange
        String otp = "123456";
        VerificationToken expiredToken = new VerificationToken(otp, testUser);
        expiredToken.setExpiryDate(LocalDateTime.now().minusMinutes(30));
        when(userRepository.findByMobileNumber("1234567890")).thenReturn(Optional.of(testUser));
        when(tokenRepository.findByUser(testUser)).thenReturn(Optional.of(expiredToken));

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> authService.verifyMobile("1234567890", otp));
        assertEquals("Verification token has expired. Please request a new one.", exception.getMessage());
        verify(tokenRepository).delete(expiredToken);
    }
}