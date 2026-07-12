package com.deevyanshu.inventory.kafka;

import com.deevyanshu.inventory.model.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;

import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class InventoryConsumer {
    // Retries 4 times: 2s, 4s, 8s, 16s. Total ~30 seconds of retries.
    // Creates topics: payment-completed-retry-0, retry-1... and payment-completed-dlt
    @RetryableTopic(
            attempts = "4",
            backOff = @BackOff(delay = 2000, multiplier = 2.0),
            autoCreateTopics = "true",
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            exclude = {IllegalArgumentException.class} // Missing products shouldn't be retried
    )
    @KafkaListener(topics = "payment-completed", groupId = "inventory-group")
    public void deductInventory(PaymentEvent event, @Header(KafkaHeaders.RECEIVED_PARTITION) int partition) {
        log.info("📦 Deducting inventory for Order: {} (Partition: {})", event.getOrderId(), partition);

        // 1. Simulate Business Rule Error (Goes to DLT instantly)
        if (event.getOrderId().equals("999")) {
            throw new IllegalArgumentException("Product ID not found in catalog");
        }

        // 2. Simulate Transient DB/Network Timeout (Will trigger Retries)
        if (Math.random() > 0.6) {
            log.warn("Database lock detected! Throwing exception to trigger Kafka Retry...");
            throw new RuntimeException("DB Connection Timeout");
        }

        log.info("✅ Inventory deducted successfully for Order: {}", event.getOrderId());
    }

    // This handles messages that completely failed all retries
    @DltHandler
    public void handleDlt(PaymentEvent event,
                          @Header(KafkaHeaders.EXCEPTION_MESSAGE) String error,
                          @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("🚨 DLT ALERT: Inventory deduction completely failed for Order {}. Origin Topic: {}. Error: {}",
                event.getOrderId(), topic, error);

        // Production Action: Send Slack alert, page on-call dev, or save to 'manual_review' table.
    }
}
