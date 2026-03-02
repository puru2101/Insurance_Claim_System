package com.insurance.policy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Policy Entity
 *
 * Represents an insurance policy held by a customer.
 * This is the "contract" that governs what claims can be filed and how much
 * can be paid out.
 *
 * Key relationship with claim-service:
 *   - claim-service stores policyNumber on each Claim
 *   - claim-service can call policy-service to validate coverage
 *   - policy-service listens to claim.events (Kafka) to track utilization
 */
@Entity
@Table(name = "policies", indexes = {
    @Index(name = "idx_policy_number",     columnList = "policy_number",   unique = true),
    @Index(name = "idx_policy_holder_id",  columnList = "policy_holder_id"),
    @Index(name = "idx_policy_status",     columnList = "status"),
    @Index(name = "idx_policy_expiry",     columnList = "expiry_date")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_number", nullable = false, unique = true, length = 50)
    private String policyNumber;

    /** Auth user ID of the policy holder */
    @Column(name = "policy_holder_id", nullable = false)
    private Long policyHolderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "policy_type", nullable = false, length = 30)
    private PolicyType policyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PolicyStatus status = PolicyStatus.ACTIVE;

    // ── Financial ─────────────────────────────────────────────────────────────

    @Column(name = "sum_insured", nullable = false, precision = 15, scale = 2)
    private BigDecimal sumInsured;

    @Column(name = "premium_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal premiumAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "premium_frequency", nullable = false, length = 15)
    @Builder.Default
    private PremiumFrequency premiumFrequency = PremiumFrequency.ANNUAL;

    @Column(name = "deductible", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal deductible = BigDecimal.ZERO;

    /** Amount already claimed / utilized against this policy */
    @Column(name = "claimed_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal claimedAmount = BigDecimal.ZERO;

    // ── Dates ─────────────────────────────────────────────────────────────────

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "last_premium_paid_date")
    private LocalDate lastPremiumPaidDate;

    @Column(name = "next_premium_due_date")
    private LocalDate nextPremiumDueDate;

    // ── Coverage details ──────────────────────────────────────────────────────

    @Column(name = "coverage_details", columnDefinition = "TEXT")
    private String coverageDetails;

    @Column(name = "exclusions", columnDefinition = "TEXT")
    private String exclusions;

    @Column(name = "nominee_name", length = 100)
    private String nomineeName;

    @Column(name = "nominee_relationship", length = 50)
    private String nomineeRelationship;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Computed helper ───────────────────────────────────────────────────────

    public BigDecimal getRemainingCoverage() {
        return sumInsured.subtract(claimedAmount);
    }

    public boolean isExpired() {
        return expiryDate.isBefore(LocalDate.now());
    }

    public boolean isActive() {
        return status == PolicyStatus.ACTIVE && !isExpired();
    }

    // ── Enums ─────────────────────────────────────────────────────────────────

    public enum PolicyType {
        HEALTH, MOTOR, LIFE, PROPERTY, TRAVEL, PERSONAL_ACCIDENT, LIABILITY, COMPREHENSIVE
    }

    public enum PolicyStatus {
        ACTIVE, EXPIRED, CANCELLED, LAPSED, PENDING_RENEWAL, SUSPENDED
    }

    public enum PremiumFrequency {
        MONTHLY, QUARTERLY, SEMI_ANNUAL, ANNUAL
    }
}
