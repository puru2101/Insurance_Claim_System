package com.insurance.auth.repository;

import com.insurance.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsernameOrEmail(String username, String email);

    Optional<User> findByRefreshToken(String refreshToken);

    Optional<User> findByPasswordResetToken(String token);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.lastLoginAt = :loginAt WHERE u.id = :userId")
    void updateLastLoginAt(@Param("userId") Long userId, @Param("loginAt") LocalDateTime loginAt);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.refreshToken = :token WHERE u.id = :userId")
    void updateRefreshToken(@Param("userId") Long userId, @Param("token") String token);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.password = :password, u.passwordResetToken = null, u.passwordResetTokenExpiry = null WHERE u.id = :userId")
    void updatePassword(@Param("userId") Long userId, @Param("password") String password);
}
