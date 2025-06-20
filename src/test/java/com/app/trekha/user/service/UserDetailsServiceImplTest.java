package com.app.trekha.user.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.app.trekha.user.model.ERole;
import com.app.trekha.user.model.Role;
import com.app.trekha.user.model.User;
import com.app.trekha.user.repository.UserRepository;

class UserDetailsServiceImplTest {

    private UserRepository userRepository;
    private UserDetailsServiceImpl userDetailsService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userDetailsService = new UserDetailsServiceImpl(userRepository);
    }

    @Test
    void loadUserByUsername_FindsByEmail_ReturnsUser() {
        User user = new User();
        user.setEmail("test@example.com");
        Role role = new Role();
        role.setName(ERole.ROLE_PASSENGER);
        user.setRoles(Set.of(role));

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername("test@example.com");
        assertNotNull(result);
        assertEquals(user, result);
    }

    @Test
    void loadUserByUsername_FindsByMobile_ReturnsUser() {
        User user = new User();
        user.setMobileNumber("1234567890");
        Role role = new Role();
        role.setName(ERole.ROLE_PASSENGER);
        user.setRoles(Set.of(role));

        when(userRepository.findByEmail("1234567890")).thenReturn(Optional.empty());
        when(userRepository.findByMobileNumber("1234567890")).thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername("1234567890");
        assertNotNull(result);
        assertEquals(user, result);
    }

    @Test
    void loadUserByUsername_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail("notfound")).thenReturn(Optional.empty());
        when(userRepository.findByMobileNumber("notfound")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername("notfound"));
    }

}