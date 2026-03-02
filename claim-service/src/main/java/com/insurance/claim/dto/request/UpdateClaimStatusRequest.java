package com.insurance.claim.dto.request;

import com.insurance.claim.entity.Claim;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class UpdateClaimStatusRequest {

    @NotNull(message = "New status is required")
    private Claim.ClaimStatus newStatus;

    @Size(max = 500)
    private String comment;

    // Required when status = APPROVED
    @DecimalMin(value = "0.01")
    private BigDecimal approvedAmount;

    // Required when status = REJECTED
    @Size(max = 500)
    private String rejectionReason;

    // Required when status = SETTLED
    @DecimalMin(value = "0.01")
    private BigDecimal settledAmount;

    @Size(max = 2000)
    private String settlementNotes;

    // Required when status = PENDING_INFO
    @Size(max = 500)
    private String infoRequestedDetails;
}
