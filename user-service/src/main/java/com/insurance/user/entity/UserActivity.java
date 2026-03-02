package com.insurance.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_activities", indexes = {
    @Index(name = "idx_activity_user_id", columnList = "auth_user_id"),
    @Index(name = "idx_activity_created_at", columnList = "created_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class UserActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "auth_user_id", nullable = false)
    private Long authUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 50)
    private ActivityType activityType;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum ActivityType {
        PROFILE_VIEWED,
        PROFILE_UPDATED,
        DOCUMENT_UPLOADED,
        CLAIM_SUBMITTED,
        CLAIM_VIEWED,
        POLICY_VIEWED,
        NOTIFICATION_SETTINGS_CHANGED,
        ACCOUNT_DEACTIVATED
    }
}
