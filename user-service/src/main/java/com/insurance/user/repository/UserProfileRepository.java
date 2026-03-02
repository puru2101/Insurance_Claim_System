package com.insurance.user.repository;

import com.insurance.user.entity.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    Optional<UserProfile> findByAuthUserId(Long authUserId);

    Optional<UserProfile> findByEmail(String email);

    Optional<UserProfile> findByPolicyNumber(String policyNumber);

    boolean existsByAuthUserId(Long authUserId);

    boolean existsByEmail(String email);

    // Admin: search users by name or email (case-insensitive)
    @Query("SELECT u FROM UserProfile u WHERE " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.lastName)  LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.email)     LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<UserProfile> searchUsers(@Param("query") String query, Pageable pageable);

    Page<UserProfile> findByAccountStatus(UserProfile.AccountStatus status, Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE UserProfile u SET u.accountStatus = :status WHERE u.authUserId = :authUserId")
    void updateAccountStatus(@Param("authUserId") Long authUserId,
                             @Param("status") UserProfile.AccountStatus status);

    @Modifying
    @Transactional
    @Query("UPDATE UserProfile u SET u.profilePictureUrl = :url WHERE u.authUserId = :authUserId")
    void updateProfilePicture(@Param("authUserId") Long authUserId, @Param("url") String url);
}
