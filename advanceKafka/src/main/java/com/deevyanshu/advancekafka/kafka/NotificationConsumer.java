package com.deevyanshu.advancekafka.kafka;

import com.deevyanshu.advancekafka.Model.NotificationLog;
import com.deevyanshu.advancekafka.Model.NotificationRequest;
import com.deevyanshu.advancekafka.configuration.KafkaConfig;
import com.deevyanshu.advancekafka.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationRepository repository;

    @KafkaListener(topics = KafkaConfig.NOTIFICATION_TOPIC, groupId = "notification-group")
    public void consume(NotificationRequest request,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition, Acknowledgment ack) {

        // Notice we log the Thread name. With concurrency=3, you will see 3 different threads!
        log.info("[Thread: {}] [Partition: {}] Processing notification for User: {}",
                Thread.currentThread().getName(), partition, request.getUserId());

        // 1. Simulate a fatal error (Routes immediately to DLQ, skipping retries)
        if (request.getMessage().contains("FAIL_NOW")) {
            throw new IllegalArgumentException("Invalid message format detected.");
        }

        // 2. Simulate a temporary network error (Will retry 3 times based on DefaultErrorHandler)
        if (request.getMessage().contains("TIMEOUT")) {
            log.warn("Network timeout! Triggering retry...");
            throw new RuntimeException("Temporary Network Issue");
        }

        // Save Success
        NotificationLog logEntry = new NotificationLog();
        logEntry.setUserId(request.getUserId());
        logEntry.setMessage(request.getMessage());
        logEntry.setStatus("SUCCESS");
        repository.save(logEntry);

        ack.acknowledge(); // Manually acknowledge the message after successful processing to commit the offset manually. Although Spring Kafka can auto-acknowledge, manual acknowledgment is often preferred for better control in production scenarios.

        log.info("✅ Notification saved successfully.");
    }

    // -----------------------------------------------------
    // Dead Letter Queue Listener
    // -----------------------------------------------------
    // although we don't need to manually create a DLT topic, Spring Kafka automatically creates it for us when the first message is sent to it. The topic name is derived from the original topic name with a ".DLT" suffix. but we do not use it in production, we will create a separate topic for DLT in production.
    @KafkaListener(topics = KafkaConfig.NOTIFICATION_TOPIC + ".DLT", groupId = "notification-dlt-group")
    public void consumeDlt(NotificationRequest request,
                           @Header(KafkaHeaders.EXCEPTION_MESSAGE) String errorMsg) {

        log.error("❌ DLQ TRIGGERED: Saving failure to database for manual review. User: {} | Error: {}",
                request.getUserId(), errorMsg);

        NotificationLog logEntry = new NotificationLog();
        logEntry.setUserId(request.getUserId());
        logEntry.setMessage(request.getMessage());
        logEntry.setStatus("FAILED: " + errorMsg);
        repository.save(logEntry);
    }
}
