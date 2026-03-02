package com.insurance.claim.dto.response;

import com.insurance.claim.entity.Claim;
import com.insurance.claim.entity.ClaimDocument;
import com.insurance.claim.entity.ClaimNote;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ClaimSummaryResponse {
    private Long id;
    private String claimNumber;
    private Claim.ClaimType claimType;
    private Claim.ClaimStatus status;
    private String statusLabel;
    private LocalDate incidentDate;
    private BigDecimal claimedAmount;
    private BigDecimal approvedAmount;
    private Claim.Priority priority;
    private Long assignedAgentId;
    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;
    private int documentCount;
}
