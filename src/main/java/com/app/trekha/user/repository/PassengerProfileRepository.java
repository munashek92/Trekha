package com.app.trekha.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.trekha.user.model.PassengerProfile;

public interface PassengerProfileRepository extends JpaRepository<PassengerProfile, Long> {
    // Custom query methods if needed
}
