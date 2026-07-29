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
import tools.jackson.databind.ObjectMapper;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationRepository repository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaConfig.NOTIFICATION_TOPIC, groupId = "notification-group-v1")
    public void consume(NotificationRequest request,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition, Acknowledgment ack) {

        try {
            log.info("[Thread: {}] [Partition: {}] Processing notification for User: {}",
                    Thread.currentThread().getName(), partition, request != null ? request.getUserId() : "NULL_OBJECT");

            // 1. Simulate a fatal error
            if (request.getMessage() != null && request.getMessage().contains("FAIL_NOW")) {
                throw new IllegalArgumentException("Invalid message format detected.");
            }

            // 2. Simulate a temporary network error
            if (request.getMessage() != null && request.getMessage().contains("TIMEOUT")) {
                log.warn("Network timeout! Triggering retry...");
                throw new RuntimeException("Temporary Network Issue");
            }

            // Save Success
            NotificationLog logEntry = new NotificationLog();
            logEntry.setUserId(request.getUserId());
            logEntry.setMessage(request.getMessage());
            logEntry.setStatus("SUCCESS");
            repository.save(logEntry);

            ack.acknowledge();
            log.info("✅ Notification saved successfully.");

        } catch (Exception e) {
            log.error("❌ CRITICAL CONSUMER CRASH CAUGHT manually: ", e);
            throw e; // Rethrow to let your DefaultErrorHandler handle it
        }
    }

    // -----------------------------------------------------
    // Dead Letter Queue Listener
    // -----------------------------------------------------
    // although we don't need to manually create a DLT topic, Spring Kafka automatically creates it for us when the first message is sent to it. The topic name is derived from the original topic name with a ".DLT" suffix. but we do not use it in production, we will create a separate topic for DLT in production.
    @KafkaListener(
            topics = KafkaConfig.NOTIFICATION_TOPIC + ".DLT",
            groupId = "notification-dlt-group-v2" // BUMPED Group ID to skip the stuck loop state
    )
    public void consumeDlt(String rawPayload, // FIXED: Changed from NotificationRequest to String for safety
                           @Header(value = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String errorMsg) {
        String safeErrorMsg = (errorMsg != null) ? errorMsg : "Unknown error context";
        log.error("❌ DLQ TRIGGERED: Logged raw poison pill from Kafka. Error: {} | Payload: {}", safeErrorMsg, rawPayload);

        NotificationLog logEntry = new NotificationLog();
        logEntry.setStatus("FAILED: " + (safeErrorMsg.length() > 200 ? safeErrorMsg.substring(0, 200) : safeErrorMsg));

        try {
            // Attempt to extract properties safely only if the payload is valid JSON
            NotificationRequest request = objectMapper.readValue(rawPayload, NotificationRequest.class);
            logEntry.setUserId(request.getUserId());
            logEntry.setMessage(request.getMessage());
        } catch (Exception ex) {
            // Fallback strategy if the message is completely broken plain text or old corrupt data
            logEntry.setUserId("UNKNOWN_USER");
            logEntry.setMessage(rawPayload.length() > 255 ? rawPayload.substring(0, 255) : rawPayload);
            log.warn("Could not parse raw DLQ message payload to object structure. Storing as raw text.");
        }

        repository.save(logEntry);
        log.info("💾 Poison pill metadata recorded successfully to H2 database.");
    }
}
