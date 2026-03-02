package com.insurance.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaPublisherService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC_PROFILE_UPDATED  = "user.profile.updated";
    private static final String TOPIC_USER_DEACTIVATED = "user.profile.deactivated";

    @Async
    public void publishProfileUpdated(Long userId, String email) {
        sendEvent(TOPIC_PROFILE_UPDATED, String.valueOf(userId), Map.of(
            "eventType", "PROFILE_UPDATED",
            "userId", userId,
            "email", email,
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    @Async
    public void publishUserDeactivated(Long userId, String email) {
        sendEvent(TOPIC_USER_DEACTIVATED, String.valueOf(userId), Map.of(
            "eventType", "USER_DEACTIVATED",
            "userId", userId,
            "email", email,
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    private void sendEvent(String topic, String key, Object payload) {
        try {
            kafkaTemplate.send(topic, key, payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish to [{}]: {}", topic, ex.getMessage());
                    } else {
                        log.debug("Published to [{}] offset [{}]", topic,
                            result.getRecordMetadata().offset());
                    }
                });
        } catch (Exception e) {
            log.error("Kafka error for topic [{}]: {}", topic, e.getMessage());
        }
    }
}
