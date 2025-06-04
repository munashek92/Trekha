package com.app.trekha.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.trekha.user.model.ERole;
import com.app.trekha.user.model.Role;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(ERole name);
}
