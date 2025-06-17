package com.app.trekha.config;

import com.app.trekha.user.model.ERole;
import com.app.trekha.user.model.Role;
import com.app.trekha.user.model.User;
import com.app.trekha.user.model.PassengerProfile;
import com.app.trekha.user.model.RegistrationMethod;
import com.app.trekha.user.repository.RoleRepository;
import com.app.trekha.user.repository.UserRepository;
import com.app.trekha.user.repository.PassengerProfileRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;

@Component
public class DataInitializer implements CommandLineRunner {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PassengerProfileRepository passengerProfileRepository;

    public DataInitializer(RoleRepository roleRepository, UserRepository userRepository, PasswordEncoder passwordEncoder, PassengerProfileRepository passengerProfileRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passengerProfileRepository = passengerProfileRepository;
    }

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);


    @Override
    public void run(String... args) throws Exception {
        // Initialize Roles
        Arrays.stream(ERole.values()).forEach(eRole -> {
            if (roleRepository.findByName(eRole).isEmpty()) {
                roleRepository.save(new Role(eRole));
                logger.info("Created role: {}", eRole.name());
            }
        });

        // Initialize a sample passenger user
        createPassenger();
    }

    private void createPassenger() {
        String newEmail = "petergreg@gmail.com";
        String newMobile = "+2793574833";

        if (userRepository.findByEmail(newEmail).isEmpty() && userRepository.findByMobileNumber(newMobile).isEmpty()) {
            User passUser = new User();
            passUser.setEmail(newEmail);
            passUser.setMobileNumber(newMobile);
            passUser.setPasswordHash(passwordEncoder.encode("^V0ZJ\\{kJ4m4"));
            passUser.setRegistrationMethod(RegistrationMethod.EMAIL); // Or MOBILE
            passUser.setActive(true);
            passUser.setEmailVerified(true); // For easy testing
            passUser.setMobileVerified(true); // For easy testing
            passUser.setCreatedAt(LocalDateTime.now());
            passUser.setUpdatedAt(LocalDateTime.now());

            Role passengerRole = roleRepository.findByName(ERole.ROLE_PASSENGER)
                    .orElseThrow(() -> new RuntimeException("Error: Role PASSENGER is not found. Initialize roles first."));
            passUser.getRoles().add(passengerRole);

            User savedUser = userRepository.save(passUser);
            logger.info("Created sample passenger user: {}" , savedUser.getEmail());

            // Create a corresponding passenger profile
            PassengerProfile passengerProfile = new PassengerProfile();
            passengerProfile.setUser(savedUser); // Links to the User
            passengerProfile.setFirstName("Sample");
            passengerProfile.setLastName("Passenger");
            passengerProfile.setCreatedAt(LocalDateTime.now());
            passengerProfile.setUpdatedAt(LocalDateTime.now());
            passengerProfile.setOnboardingCompleted(true); // Or false if you want to test onboarding
            passengerProfileRepository.save(passengerProfile);

            logger.info("Created profile for sample passenger: {}" , savedUser.getEmail());
        }
    }
}
