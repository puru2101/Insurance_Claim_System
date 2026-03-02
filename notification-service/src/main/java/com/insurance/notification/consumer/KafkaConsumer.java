package com.insurance.notification.consumer;

import com.insurance.notification.service.impl.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * KafkaConsumer
 *
 * Consumes events from all other services and dispatches to NotificationService.
 *
 * DESIGN DECISION — Why one consumer class for all topics?
 *   - Notification logic is cohesive: all we do is send messages
 *   - Easier to see all notification triggers in one place
 *   - Separate @KafkaListener per topic for independent offset tracking
 *
 * Error handling strategy:
 *   - Log and continue (don't rethrow) — a failed notification must never
 *     break the consumer's ability to process subsequent messages
 *   - Failed notifications are persisted in notification_logs with status=FAILED
 *     for monitoring and manual retry
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaConsumer {

    private final NotificationService notificationService;

    // ── Auth events ───────────────────────────────────────────────────────────

    @KafkaListener(topics = "auth.user.registered", groupId = "notification-service-group")
    public void onUserRegistered(@Payload Map<String, Object> event,
                                 @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            log.info("Received [{}] event: {}", topic, event.get("eventType"));
            String email     = (String) event.get("email");
            String firstName = (String) event.getOrDefault("firstName", "Customer");
            notificationService.sendWelcomeEmail(email, firstName);
        } catch (Exception e) {
            log.error("Error processing auth.user.registered: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "auth.user.passwordResetRequested", groupId = "notification-service-group")
    public void onPasswordResetRequested(@Payload Map<String, Object> event) {
        try {
            String email      = (String) event.get("email");
            String resetToken = (String) event.get("resetToken");
            notificationService.sendPasswordResetEmail(email, resetToken);
        } catch (Exception e) {
            log.error("Error processing password reset event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "auth.user.passwordChanged", groupId = "notification-service-group")
    public void onPasswordChanged(@Payload Map<String, Object> event) {
        try {
            String email = (String) event.get("email");
            notificationService.sendPasswordChangedEmail(email);
        } catch (Exception e) {
            log.error("Error processing password changed event: {}", e.getMessage(), e);
        }
    }

    // ── Claim events ──────────────────────────────────────────────────────────

    @KafkaListener(topics = "claim.events", groupId = "notification-service-group")
    public void onClaimEvent(@Payload Map<String, Object> event,
                             @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            String eventType  = (String) event.getOrDefault("eventType", "");
            String claimNumber = (String) event.get("claimNumber");

            log.info("Received claim event [{}] for claim: {}", eventType, claimNumber);

            // We need the customer's email — in a full implementation this would
            // be stored in the event payload or fetched from user-service.
            // For now we use a placeholder; in production enrich the Kafka event
            // in claim-service with the customer email.
            String customerEmail = (String) event.getOrDefault("customerEmail",
                "customer-" + event.get("customerId") + "@placeholder.com");

            String claimType = event.get("claimType") != null
                ? event.get("claimType").toString() : "GENERAL";

            switch (eventType) {
                case "CLAIM_SUBMITTED" ->
                    notificationService.sendClaimSubmittedEmail(customerEmail, claimNumber, claimType);

                case "STATUS_CHANGED" -> {
                    String prev    = (String) event.getOrDefault("previousStatus", "");
                    String current = (String) event.getOrDefault("newStatus", "");
                    String comment = (String) event.get("comment");
                    notificationService.sendClaimStatusChangedEmail(customerEmail, claimNumber, prev, current, comment);
                }

                default -> log.debug("No notification configured for event type: {}", eventType);
            }

        } catch (Exception e) {
            log.error("Error processing claim event: {}", e.getMessage(), e);
        }
    }
}
