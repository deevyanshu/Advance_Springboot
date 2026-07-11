package com.deevyanshu.order.Model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Data
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String aggregateId;
    private String topic;
    @Column(columnDefinition = "TEXT")
    private String payload;
    private boolean processed = false;
}
