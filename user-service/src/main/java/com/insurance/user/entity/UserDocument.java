package com.insurance.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_documents", indexes = {
    @Index(name = "idx_doc_user_id", columnList = "auth_user_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class UserDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "auth_user_id", nullable = false)
    private Long authUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 30)
    private DocumentType documentType;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column(name = "verified_by")
    private Long verifiedBy;     // admin user ID who verified

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "rejection_reason", length = 300)
    private String rejectionReason;

    @CreationTimestamp
    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt;

    public enum DocumentType {
        NATIONAL_ID,
        PASSPORT,
        DRIVING_LICENSE,
        PROOF_OF_ADDRESS,
        INCOME_PROOF,
        MEDICAL_CERTIFICATE,
        POLICY_DOCUMENT,
        OTHER
    }

    public enum VerificationStatus {
        PENDING,
        VERIFIED,
        REJECTED
    }
}
