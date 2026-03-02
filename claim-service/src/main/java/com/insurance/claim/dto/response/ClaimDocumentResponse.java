package com.insurance.claim.dto.response;

import com.insurance.claim.entity.ClaimDocument;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ClaimDocumentResponse {
    private Long id;
    private ClaimDocument.DocumentType documentType;
    private String fileName;
    private String fileUrl;
    private Long fileSizeBytes;
    private String mimeType;
    private String description;
    private Long uploadedByUserId;
    private LocalDateTime uploadedAt;
}
