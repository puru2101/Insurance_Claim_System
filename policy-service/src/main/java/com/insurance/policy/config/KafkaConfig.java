package com.insurance.policy.config;

import com.insurance.policy.service.impl.PolicyServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableAsync
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "policy-service-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "java.util.HashMap");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}

@Slf4j
@Component
@RequiredArgsConstructor
class PolicyClaimEventConsumer {

    private final PolicyServiceImpl policyService;

    /**
     * When a claim is SETTLED, update the policy's claimed amount.
     * This keeps policy utilization accurate.
     */
    @KafkaListener(topics = "claim.events", groupId = "policy-service-group")
    public void onClaimEvent(Map<String, Object> event) {
        try {
            String eventType = (String) event.getOrDefault("eventType", "");
            if (!"STATUS_CHANGED".equals(eventType)) return;

            String newStatus = (String) event.getOrDefault("newStatus", "");
            if (!"SETTLED".equals(newStatus)) return;

            String policyNumber = (String) event.get("policyNumber");
            Object settledAmtObj = event.get("settledAmount");
            if (policyNumber == null || settledAmtObj == null) return;

            BigDecimal settledAmount = new BigDecimal(settledAmtObj.toString());
            policyService.updateClaimedAmount(policyNumber, settledAmount);
            log.info("Policy {} utilization updated after claim settlement: +{}", policyNumber, settledAmount);

        } catch (Exception e) {
            log.error("Error processing claim event in policy-service: {}", e.getMessage());
        }
    }
}
