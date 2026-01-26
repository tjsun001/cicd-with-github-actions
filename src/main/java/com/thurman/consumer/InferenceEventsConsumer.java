package com.thurman.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InferenceEventsConsumer {

    private final ObjectMapper objectMapper;

    // Minimal DTO matching your produced JSON
    public record InferenceServedEvent(
            String event_id,
            String event_type,
            String user_id,
            Integer latency_ms,
            List<Integer> recommendations
    ) {}

    @KafkaListener(
            topics = "${KAFKA_TOPIC_INFERENCE_EVENTS:inference.events.v1}",
            groupId = "${KAFKA_CONSUMER_GROUP_ID_INFERENCE:inference-consumer-v1}"
    )
    public void onMessage(ConsumerRecord<String, String> record, Acknowledgment ack) throws Exception {
        InferenceServedEvent evt = objectMapper.readValue(record.value(), InferenceServedEvent.class);

        log.info("Inference event received: event_id={} type={} user_id={} latency_ms={} recs={} topic={} partition={} offset={}",
                evt.event_id(), evt.event_type(), evt.user_id(), evt.latency_ms(), evt.recommendations(),
                record.topic(), record.partition(), record.offset());

        // For now: just log + ack
        ack.acknowledge();
    }
}
