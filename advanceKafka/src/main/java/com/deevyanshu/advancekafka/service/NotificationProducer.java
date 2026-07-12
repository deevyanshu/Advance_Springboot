package com.deevyanshu.advancekafka.service;

import com.deevyanshu.advancekafka.Model.NotificationRequest;
import com.deevyanshu.advancekafka.configuration.KafkaConfig;
import com.deevyanshu.advancekafka.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendNotification(NotificationRequest request) {
        // IMPORTANT: We use the 'userId' as the Kafka Key.
        // Kafka guarantees that messages with the same key always go to the same partition!
        kafkaTemplate.send(KafkaConfig.NOTIFICATION_TOPIC, request.getUserId(), request)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Sent to Partition: {} | Offset: {} | Key: {}",
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset(),
                                request.getUserId());
                    }
                });
    }
}
