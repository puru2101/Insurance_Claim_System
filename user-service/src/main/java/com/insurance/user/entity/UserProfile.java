package com.insurance.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * UserProfile Entity
 *
 * The user-service owns its own database (insurance_user_db).
 * It stores extended user profile data — things auth-service doesn't need.
 *
 * WHY separate from auth-service User entity?
 * - auth-service is focused on IDENTITY: who are you? are you allowed in?
 * - user-service is focused on PROFILE: what are your details?
 * - Separation keeps each service small, focused, independently deployable.
 *
 * The link between them is userId — the same ID that auth-service assigns.
 * user-service TRUSTS the userId injected by the gateway (X-Auth-User-Id header).
 */
@Entity
@Table(name = "user_profiles", indexes = {
    @Index(name = "idx_user_profile_auth_user_id", columnList = "auth_user_id", unique = true),
    @Index(name = "idx_user_profile_email", columnList = "email", unique = true),
    @Index(name = "idx_user_profile_policy_number", columnList = "policy_number")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user ID from auth-service.
     * This is the FOREIGN KEY between services (logical, not DB foreign key).
     * Never changes once set.
     */
    @Column(name = "auth_user_id", nullable = false, unique = true)
    private Long authUserId;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    // ── Address ──────────────────────────────────────────────────────────────

    @Column(name = "address_line1", length = 200)
    private String addressLine1;

    @Column(name = "address_line2", length = 200)
    private String addressLine2;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "country", length = 100)
    @Builder.Default
    private String country = "India";

    // ── Insurance-specific fields ─────────────────────────────────────────────

    @Column(name = "policy_number", length = 50)
    private String policyNumber;

    @Column(name = "national_id", length = 50)     // Aadhar / PAN / SSN
    private String nationalId;

    @Column(name = "occupation", length = 100)
    private String occupation;

    @Column(name = "annual_income")
    private Double annualIncome;

    // ── Profile meta ─────────────────────────────────────────────────────────

    @Column(name = "profile_picture_url", length = 500)
    private String profilePictureUrl;

    @Column(name = "preferred_language", length = 10)
    @Builder.Default
    private String preferredLanguage = "en";

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 20)
    @Builder.Default
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @Column(name = "notification_email", nullable = false)
    @Builder.Default
    private Boolean notificationEmail = true;

    @Column(name = "notification_sms", nullable = false)
    @Builder.Default
    private Boolean notificationSms = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Enums ─────────────────────────────────────────────────────────────────

    public enum Gender {
        MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY
    }

    public enum AccountStatus {
        ACTIVE,
        SUSPENDED,
        PENDING_VERIFICATION,
        DEACTIVATED
    }
}
