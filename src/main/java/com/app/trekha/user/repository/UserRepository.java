package com.app.trekha.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.trekha.user.model.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByMobileNumber(String mobileNumber);
    Boolean existsByEmail(String email);
    Boolean existsByMobileNumber(String mobileNumber);
}
