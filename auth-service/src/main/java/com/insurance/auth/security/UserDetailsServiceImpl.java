package com.insurance.auth.security;

import com.insurance.auth.entity.User;
import com.insurance.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Bridges our User entity with Spring Security.
 * Spring Security calls loadUserByUsername() when authenticating.
 * We return a UserDetails object that Spring Security understands.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        // Support login by username OR email
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
            .orElseThrow(() -> new UsernameNotFoundException(
                "User not found with username or email: " + usernameOrEmail));

        // Convert our Role entities to Spring Security GrantedAuthority
        Set<GrantedAuthority> authorities = user.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority(role.getRoleName().name()))
            .collect(Collectors.toSet());

        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getEmail())   // use email as principal
            .password(user.getPassword())
            .authorities(authorities)
            .accountExpired(false)
            .accountLocked(!user.getIsActive())
            .credentialsExpired(false)
            .disabled(!user.getIsActive())
            .build();
    }
}
