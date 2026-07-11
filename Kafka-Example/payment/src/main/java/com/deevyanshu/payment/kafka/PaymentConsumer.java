package com.deevyanshu.payment.kafka;

import com.deevyanshu.payment.Model.OrderEvent;
import com.deevyanshu.payment.Model.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentConsumer {
    private final StringRedisTemplate redisTemplate;

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    @KafkaListener(topics = "order-created", groupId = "payment-group")
    public void processPayment(OrderEvent orderEvent) {
        String idempotencyKey = "payment:processed:" + orderEvent.getId();

        // Redis SETNX: Returns true ONLY if the key didn't exist before
        Boolean isNew = redisTemplate.opsForValue()
                .setIfAbsent(idempotencyKey, "true", 24, TimeUnit.HOURS);

        if (Boolean.FALSE.equals(isNew)) {
            log.warn("⚠️ Duplicate order {} detected! Skipping payment.", orderEvent.getId());
            return; // Exit early, exactly-once processing achieved!
        }

        log.info("💳 Processing payment for Order: ${}", orderEvent.getAmount());

        // Simulate Payment API Call...

        // Emit Payment Success Event
        PaymentEvent paymentEvent = new PaymentEvent(orderEvent.getId(), "SUCCESS");
        kafkaTemplate.send("payment-completed", paymentEvent.getOrderId(), paymentEvent);
    }

}
