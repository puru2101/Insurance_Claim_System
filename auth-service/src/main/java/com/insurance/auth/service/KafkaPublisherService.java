package com.insurance.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Publisher Service
 *
 * Publishes domain events to Kafka topics.
 * Other services (notification-service, user-service) subscribe to these events.
 *
 * WHY Kafka for this?
 * - Sending a welcome email shouldn't delay the register API response
 * - If notification-service is down, the message is queued and retried later
 * - Clean decoupling — auth-service doesn't know about notification-service
 *
 * Event naming convention: {domain}.{entity}.{action}
 *   e.g.,  auth.user.registered
 *          auth.user.loggedIn
 *          auth.user.passwordReset
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaPublisherService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Topic names — must match consumer configuration
    private static final String TOPIC_USER_REGISTERED     = "auth.user.registered";
    private static final String TOPIC_USER_LOGGED_IN      = "auth.user.loggedIn";
    private static final String TOPIC_PASSWORD_RESET      = "auth.user.passwordResetRequested";
    private static final String TOPIC_PASSWORD_CHANGED    = "auth.user.passwordChanged";

    @Async
    public void publishUserRegistered(Long userId, String email, String firstName) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "USER_REGISTERED");
        event.put("userId", userId);
        event.put("email", email);
        event.put("firstName", firstName);
        event.put("timestamp", LocalDateTime.now().toString());

        sendEvent(TOPIC_USER_REGISTERED, String.valueOf(userId), event);
    }

    @Async
    public void publishUserLoggedIn(Long userId, String email) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "USER_LOGGED_IN");
        event.put("userId", userId);
        event.put("email", email);
        event.put("timestamp", LocalDateTime.now().toString());

        sendEvent(TOPIC_USER_LOGGED_IN, String.valueOf(userId), event);
    }

    @Async
    public void publishPasswordResetRequested(String email, String resetToken) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "PASSWORD_RESET_REQUESTED");
        event.put("email", email);
        event.put("resetToken", resetToken);
        event.put("timestamp", LocalDateTime.now().toString());

        // Key = email (ensures ordering for same user)
        sendEvent(TOPIC_PASSWORD_RESET, email, event);
    }

    @Async
    public void publishPasswordChanged(String email) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "PASSWORD_CHANGED");
        event.put("email", email);
        event.put("timestamp", LocalDateTime.now().toString());

        sendEvent(TOPIC_PASSWORD_CHANGED, email, event);
    }

    private void sendEvent(String topic, String key, Object payload) {
        try {
            kafkaTemplate.send(topic, key, payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event to topic [{}]: {}", topic, ex.getMessage());
                    } else {
                        log.debug("Event published to topic [{}] partition [{}] offset [{}]",
                            topic,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                    }
                });
        } catch (Exception e) {
            // Kafka failure must NEVER fail the main business operation
            log.error("Kafka publish error for topic [{}]: {}", topic, e.getMessage());
        }
    }
}
