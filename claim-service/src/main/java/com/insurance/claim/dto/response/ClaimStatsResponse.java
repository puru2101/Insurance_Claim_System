package com.insurance.claim.dto.response;

import com.insurance.claim.entity.Claim;
import lombok.*;
import java.math.BigDecimal;
import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ClaimStatsResponse {
    private long totalClaims;
    private long submittedCount;
    private long underReviewCount;
    private long approvedCount;
    private long rejectedCount;
    private long settledCount;
    private long pendingInfoCount;
    private BigDecimal totalClaimedAmount;
    private BigDecimal totalApprovedAmount;
    private BigDecimal totalSettledAmount;
    private Map<String, Long> claimsByType;
    private double approvalRate;       // percentage
    private double avgProcessingDays;
}
