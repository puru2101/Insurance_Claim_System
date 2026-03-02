package com.insurance.notification.service.impl;

import com.insurance.notification.entity.NotificationLog;
import com.insurance.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender mailSender;
    private final NotificationLogRepository logRepository;

    @Value("${spring.mail.username:noreply@insurance.com}")
    private String fromEmail;

    @Value("${app.notification.email-enabled:false}")
    private boolean emailEnabled;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Async
    public void sendWelcomeEmail(String toEmail, String firstName) {
        String subject = "Welcome to InsureClaim! Your account is ready";
        String body = String.format("""
            Dear %s,
            
            Welcome to InsureClaim! Your account has been successfully created.
            
            You can now:
              ✅ Submit insurance claims
              ✅ Track claim status in real-time
              ✅ Upload supporting documents
              ✅ Communicate with your assigned agent
            
            Login at: %s/login
            
            If you did not create this account, please contact support immediately.
            
            Best regards,
            InsureClaim Team
            """, firstName, frontendUrl);

        send(toEmail, subject, body, NotificationLog.NotificationType.WELCOME_EMAIL,
            "auth-service", toEmail);
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
        String subject = "Password Reset Request - InsureClaim";
        String body = String.format("""
            Hello,
            
            We received a request to reset your InsureClaim password.
            
            Click the link below to reset your password (valid for 1 hour):
            %s
            
            If you did not request this, please ignore this email.
            Your password will remain unchanged.
            
            Best regards,
            InsureClaim Security Team
            """, resetLink);

        send(toEmail, subject, body, NotificationLog.NotificationType.PASSWORD_RESET,
            "auth-service", toEmail);
    }

    @Async
    public void sendPasswordChangedEmail(String toEmail) {
        String subject = "Your InsureClaim password was changed";
        String body = """
            Hello,
            
            Your InsureClaim account password was successfully changed.
            
            If you made this change, no further action is needed.
            
            If you did NOT make this change, please contact support immediately
            or reset your password at: %s/forgot-password
            
            Best regards,
            InsureClaim Security Team
            """.formatted(frontendUrl);

        send(toEmail, subject, body, NotificationLog.NotificationType.PASSWORD_CHANGED,
            "auth-service", toEmail);
    }

    @Async
    public void sendClaimSubmittedEmail(String toEmail, String claimNumber, String claimType) {
        String subject = "Claim Submitted Successfully - " + claimNumber;
        String body = String.format("""
            Dear Customer,
            
            Your %s insurance claim has been submitted successfully.
            
            Claim Number: %s
            Status: SUBMITTED
            
            What happens next:
              1. Our team will review your claim within 2-3 business days
              2. You may be contacted for additional information
              3. You'll receive updates at every stage
            
            Track your claim: %s/claims/%s
            
            Best regards,
            InsureClaim Claims Team
            """, claimType, claimNumber, frontendUrl, claimNumber);

        send(toEmail, subject, body, NotificationLog.NotificationType.CLAIM_SUBMITTED,
            "claim-service", claimNumber);
    }

    @Async
    public void sendClaimStatusChangedEmail(String toEmail, String claimNumber,
                                             String previousStatus, String newStatus,
                                             String comment) {
        NotificationLog.NotificationType type = resolveClaimNotifType(newStatus);
        String subject = String.format("Claim %s Update: %s", claimNumber, newStatus.replace("_", " "));
        String body = String.format("""
            Dear Customer,
            
            Your claim %s has been updated.
            
            Previous Status : %s
            Current Status  : %s
            %s
            
            Track your claim: %s/claims/%s
            
            Best regards,
            InsureClaim Claims Team
            """,
            claimNumber,
            previousStatus.replace("_", " "),
            newStatus.replace("_", " "),
            (comment != null && !comment.isBlank() ? "Agent Note     : " + comment + "\n" : ""),
            frontendUrl, claimNumber);

        send(toEmail, subject, body, type, "claim-service", claimNumber);
    }

    // ── Core send method with DB logging ─────────────────────────────────────

    private void send(String toEmail, String subject, String body,
                      NotificationLog.NotificationType type,
                      String source, String referenceId) {

        NotificationLog logEntry = NotificationLog.builder()
            .recipientEmail(toEmail)
            .notificationType(type)
            .subject(subject)
            .body(body)
            .eventSource(source)
            .referenceId(referenceId)
            .status(NotificationLog.NotificationStatus.PENDING)
            .build();

        try {
            if (emailEnabled) {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(toEmail);
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);
                logEntry.setStatus(NotificationLog.NotificationStatus.SENT);
                logEntry.setSentAt(LocalDateTime.now());
                log.info("Email sent [{}] to: {}", type, toEmail);
            } else {
                // Dev mode — log instead of sending
                log.info("📧 [DEV-EMAIL-MOCK] To: {} | Subject: {} | Type: {}", toEmail, subject, type);
                logEntry.setStatus(NotificationLog.NotificationStatus.SKIPPED);
                logEntry.setSentAt(LocalDateTime.now());
            }
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
            logEntry.setStatus(NotificationLog.NotificationStatus.FAILED);
            logEntry.setErrorMessage(e.getMessage());
        } finally {
            try { logRepository.save(logEntry); }
            catch (Exception ex) { log.error("Failed to save notification log: {}", ex.getMessage()); }
        }
    }

    private NotificationLog.NotificationType resolveClaimNotifType(String status) {
        return switch (status) {
            case "APPROVED"     -> NotificationLog.NotificationType.CLAIM_APPROVED;
            case "REJECTED"     -> NotificationLog.NotificationType.CLAIM_REJECTED;
            case "SETTLED"      -> NotificationLog.NotificationType.CLAIM_SETTLED;
            default             -> NotificationLog.NotificationType.CLAIM_STATUS_CHANGED;
        };
    }
}
