package com.insurance.claim.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ClaimNoteResponse {
    private Long id;
    private Long authorUserId;
    private String authorRole;
    private String content;
    private Boolean isInternal;
    private LocalDateTime createdAt;
}
