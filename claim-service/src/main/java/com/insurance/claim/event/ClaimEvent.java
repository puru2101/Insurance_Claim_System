package com.insurance.claim.event;

import com.insurance.claim.entity.Claim;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ClaimEvent — typed payload for all claim-related Kafka messages.
 *
 * Single event class covers all claim lifecycle events.
 * Consumers check eventType to determine what happened.
 *
 * Topics published:
 *   claim.submitted          → notify customer, assign agent queue
 *   claim.status.changed     → notify customer & agent of status change
 *   claim.approved           → trigger payment workflow
 *   claim.settled            → close claim, update policy
 *   claim.rejected           → notify customer with reason
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ClaimEvent {

    private String eventType;           // e.g. CLAIM_SUBMITTED, STATUS_CHANGED, CLAIM_SETTLED
    private Long claimId;
    private String claimNumber;
    private Long customerId;
    private Long agentId;
    private String policyNumber;
    private Claim.ClaimType claimType;
    private Claim.ClaimStatus previousStatus;
    private Claim.ClaimStatus newStatus;
    private BigDecimal claimedAmount;
    private BigDecimal approvedAmount;
    private BigDecimal settledAmount;
    private String comment;
    private String rejectionReason;
    private Long changedByUserId;
    private String changedByRole;
    private LocalDateTime eventTimestamp;
}
