package com.insurance.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_logs", indexes = {
    @Index(name = "idx_notif_recipient", columnList = "recipient_email"),
    @Index(name = "idx_notif_type",      columnList = "notification_type"),
    @Index(name = "idx_notif_created",   columnList = "created_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_email", nullable = false, length = 100)
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    @Column(name = "subject", length = 200)
    private String subject;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "event_source", length = 50)
    private String eventSource;   // e.g. "auth-service", "claim-service"

    @Column(name = "reference_id", length = 100)
    private String referenceId;   // e.g. claim number, user ID

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    public enum NotificationType {
        WELCOME_EMAIL,
        PASSWORD_RESET,
        PASSWORD_CHANGED,
        CLAIM_SUBMITTED,
        CLAIM_STATUS_CHANGED,
        CLAIM_APPROVED,
        CLAIM_REJECTED,
        CLAIM_SETTLED,
        AGENT_ASSIGNED
    }

    public enum NotificationStatus {
        PENDING, SENT, FAILED, SKIPPED
    }
}
