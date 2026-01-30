package com.thurman.outbox;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Profile("!aws")
@Component
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${KAFKA_TOPIC_INFERENCE_EVENTS:inference.events.v1}")
    private String topic;

    // How many events to process per tick
    @Value("${OUTBOX_BATCH_SIZE:20}")
    private int batchSize;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository,
                           KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Poll NEW events and publish them.
     * Note: this is an "at-least-once" publisher. If Kafka send succeeds but DB commit fails,
     * you could resend on next run. That’s acceptable for phase 2; consumers should be idempotent.
     */
    @Scheduled(fixedDelayString = "${OUTBOX_PUBLISH_DELAY_MS:2000}")
    @Transactional
    public void publishNewEvents() {
        List<OutboxEvent> batch = outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                OutboxEvent.Status.NEW,
                PageRequest.of(0, batchSize)
        );

        if (batch.isEmpty()) {
            return;
        }

        for (OutboxEvent evt : batch) {
            try {
                // Use outbox id as key for ordering/idempotency
                String key = evt.getId().toString();

                // payload is already JSON (string)
                kafkaTemplate.send(topic, key, evt.getPayload()).get();

                evt.markSent();
            } catch (Exception e) {
                evt.markFailed(safeMessage(e));
            }
        }

        // Because we're in @Transactional and entities are managed,
        // changes to evt will be flushed at commit.
        // (Calling saveAll is optional; leaving it out keeps it simple.)
    }

    private String safeMessage(Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) {
            msg = e.getClass().getSimpleName();
        }
        // Keep last_error reasonably sized
        return msg.length() > 800 ? msg.substring(0, 800) : msg;
    }
}
