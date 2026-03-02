package com.insurance.claim.dto.request;

import com.insurance.claim.entity.Claim;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SubmitClaimRequest {

    @NotBlank(message = "Policy number is required")
    @Size(max = 50)
    private String policyNumber;

    @NotNull(message = "Claim type is required")
    private Claim.ClaimType claimType;

    @NotNull(message = "Incident date is required")
    @PastOrPresent(message = "Incident date cannot be in the future")
    private LocalDate incidentDate;

    @NotBlank(message = "Incident description is required")
    @Size(min = 20, max = 2000, message = "Description must be between 20 and 2000 characters")
    private String incidentDescription;

    @Size(max = 300)
    private String incidentLocation;

    @NotNull(message = "Claimed amount is required")
    @DecimalMin(value = "1.00", message = "Claimed amount must be at least 1.00")
    @DecimalMax(value = "99999999.99", message = "Claimed amount exceeds maximum allowed")
    private BigDecimal claimedAmount;
}
