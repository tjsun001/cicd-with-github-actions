package com.thurman.consumer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_events")
public class ProcessedEventEntity {

    @Id
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt = Instant.now();

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "error")
    private String error;

    protected ProcessedEventEntity() {
        // for JPA
    }

    public static ProcessedEventEntity processed(UUID eventId) {
        ProcessedEventEntity e = new ProcessedEventEntity();
        e.eventId = eventId;
        e.status = "PROCESSED";
        return e;
    }

    public static ProcessedEventEntity failed(UUID eventId, String error) {
        ProcessedEventEntity e = new ProcessedEventEntity();
        e.eventId = eventId;
        e.status = "FAILED";
        e.error = error;
        return e;
    }

    public UUID getEventId() {
        return eventId;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public String getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }
}
