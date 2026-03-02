package com.insurance.claim.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * ClaimStatusHistory — immutable audit trail.
 * Every time a claim's status changes, we insert a new record here.
 * This gives a complete timeline of the claim's journey.
 *
 * Example timeline for CLM-2024-000042:
 *  2024-01-10 09:00  SUBMITTED       by customer (id: 42)
 *  2024-01-11 14:30  UNDER_REVIEW    by agent (id: 7)
 *  2024-01-12 10:00  PENDING_INFO    by agent (id: 7) — "Need repair estimate"
 *  2024-01-14 16:00  UNDER_REVIEW    by customer (id: 42) — "Documents uploaded"
 *  2024-01-15 11:00  APPROVED        by agent (id: 7)
 *  2024-01-16 09:00  SETTLED         by system
 */
@Entity
@Table(name = "claim_status_history", indexes = {
    @Index(name = "idx_history_claim_id", columnList = "claim_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class ClaimStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id", nullable = false)
    private Claim claim;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private Claim.ClaimStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    private Claim.ClaimStatus toStatus;

    @Column(name = "changed_by_user_id", nullable = false)
    private Long changedByUserId;

    @Column(name = "changed_by_role", length = 30)
    private String changedByRole;

    @Column(name = "comment", length = 500)
    private String comment;

    @CreationTimestamp
    @Column(name = "changed_at", updatable = false)
    private LocalDateTime changedAt;
}
