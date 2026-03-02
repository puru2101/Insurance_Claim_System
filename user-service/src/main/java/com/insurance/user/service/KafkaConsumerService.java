package com.insurance.user.service;

import com.insurance.user.dto.request.CreateProfileRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Kafka Consumer Service
 *
 * Listens to events published by auth-service.
 *
 * KEY FLOW — Why Kafka for user creation?
 *   1. User registers via auth-service → auth-service publishes "auth.user.registered"
 *   2. user-service (this consumer) receives the event
 *   3. user-service creates the UserProfile automatically
 *
 * This is EVENT-DRIVEN architecture:
 *   - auth-service doesn't call user-service directly (no coupling)
 *   - If user-service is down, the message is queued and processed when it restarts
 *   - Both services evolve independently
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final UserService userService;

    /**
     * Listen for new user registrations from auth-service.
     * Creates a UserProfile record in user-service DB.
     *
     * Message format (from auth-service KafkaPublisherService):
     * {
     *   "eventType": "USER_REGISTERED",
     *   "userId": 42,
     *   "email": "user@example.com",
     *   "firstName": "John"
     * }
     */
    @KafkaListener(
        topics = "auth.user.registered",
        groupId = "user-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleUserRegistered(Map<String, Object> event) {
        try {
            log.info("Received USER_REGISTERED event: {}", event);

            Long authUserId = Long.valueOf(event.get("userId").toString());
            String email     = (String) event.get("email");
            String firstName = (String) event.getOrDefault("firstName", "");

            // Skip if profile already exists (idempotent — safe to process twice)
            try {
                userService.getProfileByAuthUserId(authUserId);
                log.info("Profile already exists for userId: {}, skipping", authUserId);
                return;
            } catch (Exception ignored) {
                // Profile doesn't exist — proceed with creation
            }

            CreateProfileRequest request = new CreateProfileRequest();
            request.setAuthUserId(authUserId);
            request.setEmail(email);
            request.setFirstName(firstName);
            request.setLastName("");  // Will be updated when user fills profile

            userService.createProfile(request);
            log.info("UserProfile created for authUserId: {} email: {}", authUserId, email);

        } catch (Exception e) {
            log.error("Error processing USER_REGISTERED event: {} | error: {}",
                event, e.getMessage(), e);
            // In production: use DLQ (Dead Letter Queue) for failed messages
            // so they can be retried or inspected
        }
    }
}
