package com.app.trekha.passenger.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.app.trekha.common.exception.ResourceNotFoundException;
import com.app.trekha.passenger.dto.PassengerProfileUpdateRequest;
import com.app.trekha.storage.FileStorageService;
import com.app.trekha.user.dto.UserResponse;
import com.app.trekha.user.model.ERole;
import com.app.trekha.user.model.PassengerProfile;
import com.app.trekha.user.model.Role;
import com.app.trekha.user.model.User;
import com.app.trekha.user.repository.PassengerProfileRepository;
import com.app.trekha.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class PassengerServiceTest {

    @Mock
    private PassengerProfileRepository passengerProfileRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FileStorageService fileStorageService;
    @InjectMocks
    private PassengerService passengerService;

    private User testUser;
    private PassengerProfile testProfile;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setActive(true);
        testUser.setEmailVerified(true);

        Role passengerRole = new Role();
        passengerRole.setName(ERole.ROLE_PASSENGER);
        testUser.setRoles(Set.of(passengerRole));

        testProfile = new PassengerProfile();
        testProfile.setUser(testUser);
        testProfile.setFirstName("Test");
        testProfile.setLastName("User");
        testProfile.setOnboardingCompleted(false);
    }

    @Test
    void updatePassengerProfile_HappyPath_Success() throws IOException {
        PassengerProfileUpdateRequest updateRequest = new PassengerProfileUpdateRequest();
        updateRequest.setFirstName("Updated");
        updateRequest.setLastName("User");
        MultipartFile mockFile = new MockMultipartFile("profilePicture", "test.jpg", "image/jpeg", "test image".getBytes());
        updateRequest.setProfilePicture(mockFile);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passengerProfileRepository.findById(1L)).thenReturn(Optional.of(testProfile));
        when(fileStorageService.store(any(), anyString())).thenReturn("http://example.com/profile-pictures/1/test.jpg");
        when(passengerProfileRepository.save(any(PassengerProfile.class))).thenReturn(testProfile);

        UserResponse response = passengerService.updatePassengerProfile(1L, updateRequest);

        assertEquals("Updated", response.getFirstName());
        assertEquals("User", response.getLastName());
        assertEquals("http://example.com/profile-pictures/1/test.jpg", response.getProfilePictureUrl());

        ArgumentCaptor<PassengerProfile> profileCaptor = ArgumentCaptor.forClass(PassengerProfile.class);
        verify(passengerProfileRepository).save(profileCaptor.capture());
        assertEquals("Updated", profileCaptor.getValue().getFirstName());
    }

    @Test
    void updatePassengerProfile_NoProfilePicture_Success() throws IOException {
        PassengerProfileUpdateRequest updateRequest = new PassengerProfileUpdateRequest();
        updateRequest.setFirstName("Updated");
        updateRequest.setLastName("User");
        updateRequest.setProfilePicture(null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passengerProfileRepository.findById(1L)).thenReturn(Optional.of(testProfile));
        when(passengerProfileRepository.save(any(PassengerProfile.class))).thenReturn(testProfile);

        UserResponse response = passengerService.updatePassengerProfile(1L, updateRequest);

        assertEquals("Updated", response.getFirstName());
        assertEquals("User", response.getLastName());
        assertNull(response.getProfilePictureUrl());
        verify(fileStorageService, never()).store(any(), anyString());
    }

    @Test
    void updatePassengerProfile_EmptyProfilePicture_Success() throws IOException{
        PassengerProfileUpdateRequest updateRequest = new PassengerProfileUpdateRequest();
        updateRequest.setFirstName("Updated");
        updateRequest.setLastName("User");
        MultipartFile emptyFile = new MockMultipartFile("profilePicture", new byte[0]);
        updateRequest.setProfilePicture(emptyFile);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passengerProfileRepository.findById(1L)).thenReturn(Optional.of(testProfile));
        when(passengerProfileRepository.save(any(PassengerProfile.class))).thenReturn(testProfile);

        UserResponse response = passengerService.updatePassengerProfile(1L, updateRequest);

        assertEquals("Updated", response.getFirstName());
        assertEquals("User", response.getLastName());
        assertNull(response.getProfilePictureUrl());
        verify(fileStorageService, never()).store(any(), anyString());
    }

    @Test
    void updatePassengerProfile_UserNotFound_ThrowsException() {
        PassengerProfileUpdateRequest updateRequest = new PassengerProfileUpdateRequest();
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> passengerService.updatePassengerProfile(1L, updateRequest));

        verify(passengerProfileRepository, never()).save(any());
    }

    @Test
    void updatePassengerProfile_ProfileNotFound_ThrowsException() {
        PassengerProfileUpdateRequest updateRequest = new PassengerProfileUpdateRequest();
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passengerProfileRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> passengerService.updatePassengerProfile(1L, updateRequest));

        verify(passengerProfileRepository, never()).save(any());
    }

    @Test
    void updatePassengerProfile_FileStorageFails_ThrowsException() throws IOException {
        PassengerProfileUpdateRequest updateRequest = new PassengerProfileUpdateRequest();
        updateRequest.setFirstName("Updated");
        updateRequest.setLastName("User");
        MultipartFile mockFile = new MockMultipartFile("profilePicture", "test.jpg", "image/jpeg", "test image".getBytes());
        updateRequest.setProfilePicture(mockFile);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passengerProfileRepository.findById(1L)).thenReturn(Optional.of(testProfile));
        when(fileStorageService.store(any(), anyString())).thenThrow(new IOException("File storage failed"));

        assertThrows(RuntimeException.class,
                () -> passengerService.updatePassengerProfile(1L, updateRequest),
                "Failed to store profile picture");
    }

    @Test
    void updatePassengerProfile_NullRequest_ThrowsException() {
        assertThrows(NullPointerException.class,
                () -> passengerService.updatePassengerProfile(1L, null));
    }

    @Test
    void completeOnboarding_HappyPath_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passengerProfileRepository.findById(1L)).thenReturn(Optional.of(testProfile));
        when(passengerProfileRepository.save(any(PassengerProfile.class))).thenReturn(testProfile);

        UserResponse response = passengerService.completeOnboarding(1L);

        assertTrue(response.isOnboardingCompleted());

        ArgumentCaptor<PassengerProfile> profileCaptor = ArgumentCaptor.forClass(PassengerProfile.class);
        verify(passengerProfileRepository).save(profileCaptor.capture());
        assertTrue(profileCaptor.getValue().isOnboardingCompleted());
    }

    @Test
    void completeOnboarding_UserNotFound_ThrowsException() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> passengerService.completeOnboarding(1L));

        verify(passengerProfileRepository, never()).save(any());
    }

    @Test
    void completeOnboarding_ProfileNotFound_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passengerProfileRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> passengerService.completeOnboarding(1L));

        verify(passengerProfileRepository, never()).save(any());
    }

    @Test
    void getPassengerProfile_HappyPath_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passengerProfileRepository.findById(1L)).thenReturn(Optional.of(testProfile));

        UserResponse response = passengerService.getPassengerProfile(1L);

        assertEquals(1L, response.getId());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("Test", response.getFirstName());
        assertEquals("User", response.getLastName());
        assertEquals(testProfile.getProfilePictureUrl(), response.getProfilePictureUrl());
        assertEquals(testUser.isActive(), response.isActive());
        assertEquals(testUser.isEmailVerified(), response.isEmailVerified());
        assertEquals(testProfile.isOnboardingCompleted(), response.isOnboardingCompleted());
        assertTrue(response.getRoles().contains("ROLE_PASSENGER"));
    }

    @Test
    void getPassengerProfile_UserNotFound_ThrowsException() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> passengerService.getPassengerProfile(1L));
    }

    @Test
    void getPassengerProfile_ProfileNotFound_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passengerProfileRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> passengerService.getPassengerProfile(1L));
    }
}

