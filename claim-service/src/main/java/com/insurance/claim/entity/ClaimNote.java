package com.insurance.claim.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "claim_notes", indexes = {
    @Index(name = "idx_note_claim_id", columnList = "claim_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class ClaimNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id", nullable = false)
    private Claim claim;

    @Column(name = "author_user_id", nullable = false)
    private Long authorUserId;

    @Column(name = "author_role", length = 30)
    private String authorRole;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** Internal notes are visible to agents/admins only, not the customer */
    @Column(name = "is_internal", nullable = false)
    @Builder.Default
    private Boolean isInternal = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
