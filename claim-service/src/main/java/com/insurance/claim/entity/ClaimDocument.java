package com.insurance.claim.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "claim_documents", indexes = {
    @Index(name = "idx_claim_doc_claim_id", columnList = "claim_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class ClaimDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id", nullable = false)
    private Claim claim;

    @Column(name = "uploaded_by_user_id", nullable = false)
    private Long uploadedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 40)
    private DocumentType documentType;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "description", length = 300)
    private String description;

    @CreationTimestamp
    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt;

    public enum DocumentType {
        INCIDENT_PHOTO,
        POLICE_REPORT,
        MEDICAL_REPORT,
        REPAIR_ESTIMATE,
        INVOICE,
        DEATH_CERTIFICATE,
        WITNESS_STATEMENT,
        SUPPORTING_DOCUMENT,
        OTHER
    }
}
