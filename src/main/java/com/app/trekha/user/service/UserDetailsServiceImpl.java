package com.app.trekha.user.service;

import com.app.trekha.user.model.User;
import com.app.trekha.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

private final UserRepository userRepository;

@Override
@Transactional
public UserDetails loadUserByUsername(String usernameOrEmailOrMobile) throws UsernameNotFoundException {
        // Try finding by email first, then by mobile number
        User user = userRepository.findByEmail(usernameOrEmailOrMobile)
                .orElseGet(() -> userRepository.findByMobileNumber(usernameOrEmailOrMobile)
                        .orElseThrow(() -> new UsernameNotFoundException("User Not Found with identifier: " + usernameOrEmailOrMobile)));

        // List<GrantedAuthority> authorities = user.getRoles().stream()
        //         .map(role -> new SimpleGrantedAuthority(role.getName().name()))
        //         .collect(Collectors.toList());

        return user;

}
}
