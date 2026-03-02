package com.insurance.policy.dto.request;

import com.insurance.policy.entity.Policy;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreatePolicyRequest {

    @NotNull private Long policyHolderId;

    @NotNull private Policy.PolicyType policyType;

    @NotNull @DecimalMin("1000.00")
    private BigDecimal sumInsured;

    @NotNull @DecimalMin("100.00")
    private BigDecimal premiumAmount;

  //  @Builder.Default  // dummy field; real default in entity
    private Policy.PremiumFrequency premiumFrequency = Policy.PremiumFrequency.ANNUAL;

    private BigDecimal deductible;

    @NotNull @FutureOrPresent
    private LocalDate startDate;

    @NotNull @Future
    private LocalDate expiryDate;

    private String coverageDetails;
    private String exclusions;
    private String nomineeName;
    private String nomineeRelationship;

    // Use Lombok @Builder.Default only in entity; remove from DTO
    public Policy.PremiumFrequency getPremiumFrequency() {
        return premiumFrequency != null ? premiumFrequency : Policy.PremiumFrequency.ANNUAL;
    }
}
