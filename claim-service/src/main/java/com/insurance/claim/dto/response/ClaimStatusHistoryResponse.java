package com.insurance.claim.dto.response;

import com.insurance.claim.entity.Claim;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ClaimStatusHistoryResponse {
    private Long id;
    private Claim.ClaimStatus fromStatus;
    private Claim.ClaimStatus toStatus;
    private Long changedByUserId;
    private String changedByRole;
    private String comment;
    private LocalDateTime changedAt;
}
