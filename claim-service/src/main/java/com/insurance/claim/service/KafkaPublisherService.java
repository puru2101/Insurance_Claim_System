package com.insurance.claim.service;

import com.insurance.claim.event.ClaimEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Kafka Publisher Service — publishes claim lifecycle events.
 *
 * Topics:
 *   claim.submitted       → notification-service sends confirmation email
 *   claim.status.changed  → notification-service notifies customer & agent
 *   claim.approved        → could trigger payment service
 *   claim.settled         → update policy service records
 *   claim.rejected        → notification-service sends rejection email
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaPublisherService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC_CLAIM_EVENTS = "claim.events";

    @Async
    public void publishClaimEvent(ClaimEvent event) {
        try {
            // Use claimNumber as key — guarantees ordering for same claim's events
            kafkaTemplate.send(TOPIC_CLAIM_EVENTS, event.getClaimNumber(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish claim event [{}]: {}", event.getEventType(), ex.getMessage());
                    } else {
                        log.info("Published [{}] for claim {} to partition {} offset {}",
                            event.getEventType(), event.getClaimNumber(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                    }
                });
        } catch (Exception e) {
            log.error("Kafka error publishing claim event: {}", e.getMessage());
        }
    }
}
