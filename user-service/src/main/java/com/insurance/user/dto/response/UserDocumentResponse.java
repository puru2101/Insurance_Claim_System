package com.insurance.user.dto.response;

import com.insurance.user.entity.UserDocument;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserDocumentResponse {
    private Long id;
    private Long authUserId;
    private UserDocument.DocumentType documentType;
    private String fileName;
    private String fileUrl;
    private Long fileSizeBytes;
    private String mimeType;
    private UserDocument.VerificationStatus verificationStatus;
    private String rejectionReason;
    private LocalDateTime uploadedAt;
    private LocalDateTime verifiedAt;
}
