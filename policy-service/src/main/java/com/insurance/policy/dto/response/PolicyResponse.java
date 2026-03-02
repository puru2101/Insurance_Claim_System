package com.insurance.policy.dto.response;

import com.insurance.policy.entity.Policy;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PolicyResponse {
    private Long id;
    private String policyNumber;
    private Long policyHolderId;
    private Policy.PolicyType policyType;
    private Policy.PolicyStatus status;
    private BigDecimal sumInsured;
    private BigDecimal premiumAmount;
    private Policy.PremiumFrequency premiumFrequency;
    private BigDecimal deductible;
    private BigDecimal claimedAmount;
    private BigDecimal remainingCoverage;
    private LocalDate startDate;
    private LocalDate expiryDate;
    private LocalDate nextPremiumDueDate;
    private String coverageDetails;
    private String exclusions;
    private String nomineeName;
    private String nomineeRelationship;
    private boolean expired;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
