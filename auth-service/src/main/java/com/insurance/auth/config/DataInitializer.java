package com.insurance.auth.config;

import com.insurance.auth.entity.Role;
import com.insurance.auth.entity.User;
import com.insurance.auth.repository.RoleRepository;
import com.insurance.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.Set;

/**
 * DataInitializer
 *
 * Runs on startup to seed essential data:
 *  1. Creates the 4 roles if they don't exist
 *  2. Creates a default ADMIN user if no admin exists
 *
 * This ensures the application always has a working state on first boot.
 * Idempotent — safe to run multiple times.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner seedData() {
        return args -> {
            seedRoles();
            seedAdminUser();
        };
    }

    private void seedRoles() {
        Arrays.stream(Role.RoleName.values()).forEach(roleName -> {
            if (roleRepository.findByRoleName(roleName).isEmpty()) {
                roleRepository.save(Role.builder().roleName(roleName).build());
                log.info("Created role: {}", roleName);
            }
        });
    }

    private void seedAdminUser() {
        if (!userRepository.existsByEmail("admin@insurance.com")) {
            Role adminRole = roleRepository.findByRoleName(Role.RoleName.ROLE_ADMIN)
                .orElseThrow();

            User admin = User.builder()
                .username("admin")
                .email("admin@insurance.com")
                .password(passwordEncoder.encode("Admin@123!"))
                .firstName("System")
                .lastName("Admin")
                .isActive(true)
                .isEmailVerified(true)
                .roles(Set.of(adminRole))
                .build();

            userRepository.save(admin);
            log.warn("==========================================================");
            log.warn("  DEFAULT ADMIN CREATED: admin@insurance.com / Admin@123!");
            log.warn("  CHANGE THIS PASSWORD IMMEDIATELY IN PRODUCTION!");
            log.warn("==========================================================");
        }
    }
}
