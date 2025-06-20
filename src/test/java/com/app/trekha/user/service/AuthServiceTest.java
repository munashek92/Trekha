package com.app.trekha.user.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.app.trekha.config.security.JwtService;
import com.app.trekha.user.dto.JwtResponse;
import com.app.trekha.user.dto.LoginRequest;
import com.app.trekha.user.dto.PassengerRegistrationRequest;
import com.app.trekha.user.dto.UserResponse;
import com.app.trekha.user.model.ERole;
import com.app.trekha.user.model.PassengerProfile;
import com.app.trekha.user.model.RegistrationMethod;
import com.app.trekha.user.model.Role;
import com.app.trekha.user.model.User;
import com.app.trekha.user.repository.PassengerProfileRepository;
import com.app.trekha.user.repository.RoleRepository;
import com.app.trekha.user.repository.UserRepository;

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
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void registerPassenger_WithEmailRegistration_Success() {
        PassengerRegistrationRequest request = getRequest();

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");

        Role passengerRole = new Role();
        passengerRole.setName(ERole.ROLE_PASSENGER);
        when(roleRepository.findByName(ERole.ROLE_PASSENGER)).thenReturn(Optional.of(passengerRole));

        User savedUser = getSavedUser(passengerRole);

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        PassengerProfile savedProfile = getSavedProfile(savedUser);
        when(passengerProfileRepository.save(any(PassengerProfile.class))).thenReturn(savedProfile);

        UserResponse response = authService.registerPassenger(request, RegistrationMethod.EMAIL);

        assertEquals("test@example.com", response.getEmail());
        assertEquals("John", response.getFirstName());
        assertEquals("Doe", response.getLastName());
        assertFalse(response.isEmailVerified());
        assertTrue(response.isActive());
        assertTrue(response.getRoles().contains("ROLE_PASSENGER"));
    }

    @Test
    void registerPassenger_WithMobileRegistration_Success() {
        PassengerRegistrationRequest request = new PassengerRegistrationRequest();
        request.setMobileNumber("1234567890");
        request.setPassword("password");
        request.setFirstName("Jane");
        request.setLastName("Smith");

        when(userRepository.existsByMobileNumber("1234567890")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");

        Role passengerRole = new Role();
        passengerRole.setName(ERole.ROLE_PASSENGER);
        when(roleRepository.findByName(ERole.ROLE_PASSENGER)).thenReturn(Optional.of(passengerRole));

        User savedUser = new User();
        savedUser.setId(2L);
        savedUser.setMobileNumber("1234567890");
        savedUser.setRoles(Set.of(passengerRole));
        savedUser.setEmailVerified(false);
        savedUser.setMobileVerified(false);
        savedUser.setActive(true);

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        PassengerProfile savedProfile = new PassengerProfile();
        savedProfile.setUser(savedUser);
        savedProfile.setFirstName("Jane");
        savedProfile.setLastName("Smith");
        when(passengerProfileRepository.save(any(PassengerProfile.class))).thenReturn(savedProfile);

        UserResponse response = authService.registerPassenger(request, RegistrationMethod.MOBILE);

        assertEquals("1234567890", response.getMobileNumber());
        assertEquals("Jane", response.getFirstName());
        assertEquals("Smith", response.getLastName());
        assertFalse(response.isMobileVerified());
        assertTrue(response.isActive());
        assertTrue(response.getRoles().contains("ROLE_PASSENGER"));
    }

    @Test
    void registerPassenger_WithExistingEmail_ThrowsException() {
        PassengerRegistrationRequest request = new PassengerRegistrationRequest();
        request.setEmail("test@example.com");
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.registerPassenger(request, RegistrationMethod.EMAIL));
        assertEquals("Error: Email is already in use!", ex.getMessage());
    }

    @Test
    void registerPassenger_WithExistingMobile_ThrowsException() {
        PassengerRegistrationRequest request = new PassengerRegistrationRequest();
        request.setMobileNumber("1234567890");
        when(userRepository.existsByMobileNumber("1234567890")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.registerPassenger(request, RegistrationMethod.MOBILE));
        assertEquals("Error: Mobile number is already in use!", ex.getMessage());
    }

    @Test
    void registerPassenger_NullRequest_ThrowsException() {
        assertThrows(NullPointerException.class, () -> authService.registerPassenger(null, RegistrationMethod.EMAIL));
    }

    @Test
    void registerPassenger_NullRegistrationMethod_ThrowsException() {
        PassengerRegistrationRequest request = new PassengerRegistrationRequest();
        assertThrows(IllegalArgumentException.class, () -> authService.registerPassenger(request, null));
    }

    @Test
    void loginUser_Successful() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setLoginIdentifier("test@example.com");
        loginRequest.setPassword("password");

        Authentication authentication = mock(Authentication.class);
        User userPrincipal = mock(User.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);

        when(userPrincipal.getUsername()).thenReturn("test@example.com");

         // Mock the ID - this is crucial as the service method calls getId()
        when(userPrincipal.getId()).thenReturn(1L);
        // Mock authorities - keep only the relevant ones for the test assertion
    
        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_PASSENGER"));
        when(userPrincipal.getAuthorities()).thenReturn((Collection)authorities);

        when(jwtService.generateToken(userPrincipal)).thenReturn("jwt-token");


        User userEntity = new User();
        userEntity.setId(1L);
        userEntity.setEmail("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(userEntity));

        JwtResponse jwtResponse = authService.loginUser(loginRequest);

        assertEquals("jwt-token", jwtResponse.getToken());
        assertEquals(1L, jwtResponse.getId());
        assertTrue(jwtResponse.getRoles().contains("ROLE_PASSENGER"));
    }

    @Test
    void loginUser_UserNotFound_ThrowsException() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setLoginIdentifier("unknown@example.com");
        loginRequest.setPassword("password");

        Authentication authentication = mock(Authentication.class);
        User userPrincipal = mock(User.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);

        when(userPrincipal.getUsername()).thenReturn("unknown@example.com");
        Set<GrantedAuthority> authoritiesForNotFound = new HashSet<>();
        authoritiesForNotFound.add(new SimpleGrantedAuthority("ROLE_PASSENGER"));
        when(userPrincipal.getAuthorities()).thenReturn((Collection)authoritiesForNotFound);

        when(jwtService.generateToken(userPrincipal)).thenReturn("jwt-token");
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByMobileNumber("0946234657")).thenReturn(Optional.empty());

        assertThrows(org.springframework.security.core.userdetails.UsernameNotFoundException.class,
                () -> authService.loginUser(loginRequest));
    }

    @Test
    void loginUser_NullRequest_ThrowsException() {
        assertThrows(NullPointerException.class, () -> authService.loginUser(null));
    }

    private User getSavedUser(Role passengerRole) {
        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setEmail("test@example.com");
        savedUser.setRoles(Set.of(passengerRole));
        savedUser.setEmailVerified(false);
        savedUser.setMobileVerified(false);
        savedUser.setActive(true);
        return savedUser;
    }

    private PassengerRegistrationRequest getRequest() {
        PassengerRegistrationRequest request = new PassengerRegistrationRequest();
        request.setEmail("test@example.com");
        request.setPassword("password");
        request.setFirstName("John");
        request.setLastName("Doe");
        return request;
    }

    private PassengerProfile getSavedProfile(User savedUser) {
        PassengerProfile savedProfile = new PassengerProfile();
        savedProfile.setUser(savedUser);
        savedProfile.setFirstName("John");
        savedProfile.setLastName("Doe");
        return savedProfile;
    }
}
