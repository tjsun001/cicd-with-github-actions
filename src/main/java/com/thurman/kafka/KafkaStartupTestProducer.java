package com.thurman.kafka;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.kafka.startup-producer.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class KafkaStartupTestProducer implements CommandLineRunner {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${KAFKA_TOPIC_INFERENCE_EVENTS:inference.events.v1}")
    private String topic;

    public KafkaStartupTestProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void run(String... args) {
        String msg = "{\"event_id\":\"spring-test-1\",\"event_type\":\"InferenceServed\",\"user_id\":\"123\",\"latency_ms\":42,\"recommendations\":[101,102,103]}";
        kafkaTemplate.send(topic, "spring-test-1", msg);
        System.out.println("✅ Sent Kafka startup test message to topic=" + topic);
    }
}

