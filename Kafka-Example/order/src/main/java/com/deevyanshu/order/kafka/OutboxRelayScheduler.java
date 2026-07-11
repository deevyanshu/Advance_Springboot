package com.deevyanshu.order.kafka;

import com.deevyanshu.order.Model.OutboxEvent;
import com.deevyanshu.order.Repository.OutboxRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxRelayScheduler {

    private final OutboxRepository outboxRepository;

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Transactional
    @Scheduled(fixedDelay = 2000) // Every 2 seconds
    public void publishOutEvents(){
        List<OutboxEvent> events = outboxRepository.findByProcessedFalse();

        for (OutboxEvent event : events) {
            kafkaTemplate.send(event.getTopic(), event.getAggregateId(), event.getPayload())
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            event.setProcessed(true);
                            outboxRepository.save(event); // Mark as processed
                            log.info("Outbox event {} published successfully", event.getId());
                        }
                    });
        }
    }
}
