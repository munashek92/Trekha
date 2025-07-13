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
import com.app.trekha.user.dto.PasswordResetRequest;
import com.app.trekha.user.model.ERole;
import com.app.trekha.user.model.PassengerProfile;
import com.app.trekha.user.model.RegistrationMethod;
import com.app.trekha.user.model.Role;
import com.app.trekha.user.model.User;
import com.app.trekha.user.model.VerificationToken;
import com.app.trekha.user.repository.PassengerProfileRepository;
import com.app.trekha.user.repository.PasswordResetTokenRepository;
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
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private SmsService smsService;

    @InjectMocks
    private AuthService authService;

    private PassengerRegistrationRequest emailRequest;
    private PassengerRegistrationRequest mobileRequest;
    private User testUser;

    private static final String EMAIL = "test@example.com";
    private static final String MOBILE = "1234567890";


    @BeforeEach
    void setUp() {
        emailRequest = new PassengerRegistrationRequest();
        emailRequest.setEmail(EMAIL);
        emailRequest.setPassword("password123");
        emailRequest.setFirstName("Test");
        emailRequest.setLastName("User");

        mobileRequest = new PassengerRegistrationRequest();
        mobileRequest.setMobileNumber(MOBILE);
        mobileRequest.setPassword("password123");
        mobileRequest.setFirstName("Test");
        mobileRequest.setLastName("User");

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail(EMAIL);
        testUser.setMobileNumber(MOBILE);
    }

    @Test
    void registerPassengerWithEmailShouldCreateUserAndSendVerificationEmail() {
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
        verify(emailService).sendEmail(eq(EMAIL), eq("Trekha - Verify Your Email"), bodyCaptor.capture());
        assertTrue(bodyCaptor.getValue().contains("Hi Test"));
        assertTrue(bodyCaptor.getValue().contains("verify-email?token="));
        verify(smsService, never()).sendSms(any(), any());
        verify(tokenRepository).save(any(VerificationToken.class));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertFalse(userCaptor.getValue().isEmailVerified());
    }

    @Test
    void registerPassengerWithMobileShouldCreateUserAndSendVerificationSms() {
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
        verify(smsService).sendSms(eq(MOBILE), smsBodyCaptor.capture());
        assertTrue(smsBodyCaptor.getValue().startsWith("Your Trekha verification code is: "));
        verify(emailService, never()).sendEmail(any(), any(), any());
        verify(tokenRepository).save(any(VerificationToken.class));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertFalse(userCaptor.getValue().isMobileVerified());
    }

    @Test
    void verifyEmailWithValidTokenShouldSetEmailVerifiedToTrue() {
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
    void verifyEmailWithInvalidTokenShouldThrowException() {
        // Arrange
        when(tokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> authService.verifyEmail("invalid-token"));
        assertEquals("Invalid verification token.", exception.getMessage());
    }

    @Test
    void verifyEmailWithExpiredTokenShouldThrowException() {
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
    void verifyMobileWithValidOtpShouldSetMobileVerifiedToTrue() {
        // Arrange
        String otp = "123456";
        VerificationToken token = new VerificationToken(otp, testUser);
        when(userRepository.findByMobileNumber(MOBILE)).thenReturn(Optional.of(testUser));
        when(tokenRepository.findByUser(testUser)).thenReturn(Optional.of(token));

        // Act
        authService.verifyMobile(MOBILE, otp);

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertTrue(userCaptor.getValue().isMobileVerified());
        verify(tokenRepository).delete(token);
    }

    @Test
    void verifyMobileWithInvalidOtpShouldThrowException() {
        // Arrange
        String correctOtp = "123456";
        String wrongOtp = "999999";
        VerificationToken token = new VerificationToken(correctOtp, testUser);
        when(userRepository.findByMobileNumber(MOBILE)).thenReturn(Optional.of(testUser));
        when(tokenRepository.findByUser(testUser)).thenReturn(Optional.of(token));

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> authService.verifyMobile("1234567890", wrongOtp));
        assertEquals("Invalid verification code.", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void verifyMobileWithNonExistentUserShouldThrowException() {
        // Arrange
        when(userRepository.findByMobileNumber("non-existent-number")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> authService.verifyMobile("non-existent-number", "123456"));
    }

    @Test
    void verifyMobileWithNoTokenForUserShouldThrowException() {
        // Arrange
        when(userRepository.findByMobileNumber(MOBILE)).thenReturn(Optional.of(testUser));
        when(tokenRepository.findByUser(testUser)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> authService.verifyMobile("1234567890", "123456"));
        assertEquals("No verification token found for this user. Please request a new one.", exception.getMessage());
    }

    @Test
    void verifyMobileWithExpiredTokenShouldThrowException() {
        // Arrange
        String otp = "123456";
        VerificationToken expiredToken = new VerificationToken(otp, testUser);
        expiredToken.setExpiryDate(LocalDateTime.now().minusMinutes(30));
        when(userRepository.findByMobileNumber(MOBILE)).thenReturn(Optional.of(testUser));
        when(tokenRepository.findByUser(testUser)).thenReturn(Optional.of(expiredToken));

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> authService.verifyMobile("1234567890", otp));
        assertEquals("Verification token has expired. Please request a new one.", exception.getMessage());
        verify(tokenRepository).delete(expiredToken);
    }

@Test
    void requestPasswordResetWithValidEmailShouldGenerateTokenAndSendEmail() {
    // Arrange
    when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(testUser));
    testUser.setActive(true);
    testUser.setEmailVerified(true); // Assuming email verification required for password reset

    // Act
    authService.requestPasswordReset(EMAIL);

    // Assert
    verify(passwordResetTokenRepository).save(any(com.app.trekha.user.model.PasswordResetToken.class));
    ArgumentCaptor<String> emailBodyCaptor = ArgumentCaptor.forClass(String.class);
    verify(emailService).sendEmail(eq(EMAIL), eq("Trekha - Reset Your Password"), emailBodyCaptor.capture());
    assertTrue(emailBodyCaptor.getValue().contains("reset-password?token="));
    }

@Test
void requestPasswordResetWithValidMobileShouldGenerateTokenAndSendEmail() {
    // Arrange
    when(userRepository.findByMobileNumber(MOBILE)).thenReturn(Optional.of(testUser));
    testUser.setActive(true);
    testUser.setMobileVerified(true);
    testUser.setRegistrationMethod(RegistrationMethod.MOBILE);

    // Act
    authService.requestPasswordReset("1234567890");

    // Assert
    verify(passwordResetTokenRepository).save(any(com.app.trekha.user.model.PasswordResetToken.class));
    ArgumentCaptor<String> emailBodyCaptor = ArgumentCaptor.forClass(String.class);
    verify(emailService).sendEmail(eq(EMAIL), eq("Trekha - Reset Your Password"), emailBodyCaptor.capture());
    assertTrue(emailBodyCaptor.getValue().contains("reset-password?token="));
}

@Test
void requestPasswordResetWithNonExistentIdentifierShouldThrowException() {
    // Arrange
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
    when(userRepository.findByMobileNumber(anyString())).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(org.springframework.security.core.userdetails.UsernameNotFoundException.class, () -> authService.requestPasswordReset("nonexistent@example.com"));
}

@Test
void requestPasswordResetForInactiveAccountShouldThrowException() {
    // Arrange
    testUser.setActive(false);
    when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(testUser));

    // Act & Assert
    assertThrows(IllegalStateException.class, () -> authService.requestPasswordReset("test@example.com"));
}

@Test
void requestPasswordResetForUnverifiedEmailShouldThrowException() {
    // Arrange
    testUser.setActive(true);
    testUser.setEmailVerified(false);
    testUser.setRegistrationMethod(RegistrationMethod.EMAIL);
    when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(testUser));

    // Act & Assert
    assertThrows(IllegalStateException.class, () -> authService.requestPasswordReset("test@example.com"));
}

@Test
void requestPasswordResetForUnverifiedMobileShouldThrowException() {
    // Arrange
    testUser.setActive(true);
    testUser.setMobileVerified(false);
    testUser.setRegistrationMethod(RegistrationMethod.MOBILE);
    when(userRepository.findByMobileNumber(MOBILE)).thenReturn(Optional.of(testUser));

    // Act & Assert
    assertThrows(IllegalStateException.class, () -> authService.requestPasswordReset("1234567890"));
}


@Test
void resetPasswordWithValidTokenAndRequestShouldResetPassword() {
    // Arrange
    String token = "valid-reset-token";
    String newPassword = "NewPassword123";
    PasswordResetRequest request = new PasswordResetRequest();
    request.setNewPassword(newPassword);

    com.app.trekha.user.model.PasswordResetToken resetToken = new com.app.trekha.user.model.PasswordResetToken(token, testUser);
    when(passwordResetTokenRepository.findByToken(token)).thenReturn(Optional.of(resetToken));
    when(passwordEncoder.encode(newPassword)).thenReturn("encodedNewPassword");

    // Act
    authService.resetPassword(token, request);

    // Assert
    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());
    assertEquals("encodedNewPassword", userCaptor.getValue().getPasswordHash());
    verify(passwordResetTokenRepository).delete(resetToken);
}

@Test
void resetPasswordWithInvalidTokenShouldThrowException() {
    // Arrange
    String invalidToken = "invalid-token";
    PasswordResetRequest request = new PasswordResetRequest();
    request.setNewPassword("NewPassword123");
    when(passwordResetTokenRepository.findByToken(invalidToken)).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> authService.resetPassword(invalidToken, request));
}

@Test
void resetPasswordWithExpiredTokenShouldThrowException() {
    // Arrange
    String expiredTokenString = "expired-token";
    PasswordResetRequest request = new PasswordResetRequest();
    request.setNewPassword("NewPassword123");

    com.app.trekha.user.model.PasswordResetToken expiredToken = new com.app.trekha.user.model.PasswordResetToken(expiredTokenString, testUser);
    expiredToken.setExpiryDate(LocalDateTime.now().minusMinutes(1)); // Expired

    when(passwordResetTokenRepository.findByToken(expiredTokenString)).thenReturn(Optional.of(expiredToken));

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> authService.resetPassword(expiredTokenString, request));
    verify(passwordResetTokenRepository).delete(expiredToken);
}

@Test
void resetPasswordWithInvalidPasswordShouldThrowException() {
    // Arrange
    String token = "valid-token";
    PasswordResetRequest request = new PasswordResetRequest();
    request.setNewPassword("weak"); // Password not meeting criteria
    com.app.trekha.user.model.PasswordResetToken resetToken = new com.app.trekha.user.model.PasswordResetToken(token, testUser);
    when(passwordResetTokenRepository.findByToken(token)).thenReturn(Optional.of(resetToken));

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> authService.resetPassword(token, request));
    verify(userRepository, never()).save(any());
}

}