package com.insurance.claim.dto.response;

import com.insurance.claim.entity.Claim;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ClaimResponse {

    private Long id;
    private String claimNumber;
    private Long customerId;
    private Long assignedAgentId;
    private String policyNumber;
    private Claim.ClaimType claimType;
    private Claim.ClaimStatus status;
    private String statusLabel;          // Human-readable status
    private LocalDate incidentDate;
    private String incidentDescription;
    private String incidentLocation;
    private BigDecimal claimedAmount;
    private BigDecimal approvedAmount;
    private BigDecimal settledAmount;
    private BigDecimal coverageLimit;
    private Claim.Priority priority;
    private String rejectionReason;
    private String settlementNotes;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewStartedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime settledAt;
    private LocalDateTime updatedAt;
    private List<ClaimDocumentResponse> documents;
    private List<ClaimNoteResponse> notes;
    private List<ClaimStatusHistoryResponse> statusHistory;
}
