package com.app.trekha.user.model;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "email"),
        @UniqueConstraint(columnNames = "mobileNumber")
})
@Data
@NoArgsConstructor
public class User implements UserDetails{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String mobileNumber;

    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RegistrationMethod registrationMethod;

    private String socialProviderId; // For Google, Facebook, Apple ID

    private boolean isActive = true;
    private boolean isEmailVerified = false;
    private boolean isMobileVerified = false;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime lastLoginAt;

    // Convenience constructor for registration
    public User(String email, String mobileNumber, String passwordHash, RegistrationMethod registrationMethod) {
        this.email = email;
        this.mobileNumber = mobileNumber;
        this.passwordHash = passwordHash;
        this.registrationMethod = registrationMethod;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();
        for (Role role : this.roles) {
            authorities.add(new SimpleGrantedAuthority(role.getName().name()));
        }
        return authorities;
    }

    @Override
    public String getPassword() {
        return this.passwordHash; // Return the encoded password
    }

    @Override
    public String getUsername() {
        // Use email as the primary username for Spring Security, or mobile if email is null
        return this.email != null ? this.email : this.mobileNumber;
    }

    @Override
    public boolean isAccountNonExpired() { return true; } // Implement your logic if needed
    @Override
    public boolean isAccountNonLocked() { return isActive; } // Use isActive for account lock status
    @Override
    public boolean isCredentialsNonExpired() { return true; } // Implement your logic if needed
    @Override
    public boolean isEnabled() { return isActive; } // Use isActive for enabled status

}