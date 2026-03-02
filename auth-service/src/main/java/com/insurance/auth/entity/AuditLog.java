package com.insurance.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_user_id", columnList = "user_id"),
    @Index(name = "idx_audit_created_at", columnList = "created_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", length = 100)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 30)
    private AuditAction action;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 300)
    private String userAgent;

    @Column(name = "details", length = 500)
    private String details;

    @Column(name = "success", nullable = false)
    private Boolean success;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum AuditAction {
        LOGIN_SUCCESS,
        LOGIN_FAILURE,
        LOGOUT,
        REGISTER,
        TOKEN_REFRESH,
        PASSWORD_RESET_REQUEST,
        PASSWORD_RESET_SUCCESS,
        PASSWORD_CHANGE,
        ACCOUNT_LOCKED,
        EMAIL_VERIFIED
    }
}
