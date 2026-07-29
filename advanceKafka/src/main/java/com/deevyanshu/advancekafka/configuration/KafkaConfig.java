package com.deevyanshu.advancekafka.configuration;

import com.deevyanshu.advancekafka.Model.NotificationRequest;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.util.backoff.FixedBackOff;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class KafkaConfig {
    public static final String NOTIFICATION_TOPIC = "notifications-topic";

    // 1. Topic Creation (3 Partitions, 3 Replicas)
    @Bean
    public NewTopic notificationTopic() {
        return TopicBuilder.name(NOTIFICATION_TOPIC)
                .partitions(3)       // Split the load into 3 lanes
                .replicas(3)         // Copy data to all 3 brokers
                .build();
    }

    @Bean
    public NewTopic notificationDlqTopic() {
        return TopicBuilder.name(KafkaConfig.NOTIFICATION_TOPIC + ".DLT")
                .partitions(3)       // Match main topic layout
                .replicas(3)         // Match your 3-broker cluster requirement
                .build();
    }

    // 2. Global DLQ & Retry Configuration
    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<?, ?> template) {
        // Recoverer takes the failed message and sends it to a topic named {originalTopic}.DLT
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template
        , (record, ex) -> new TopicPartition(KafkaConfig.NOTIFICATION_TOPIC + ".DLT", 0));

        // Retry 3 times, with a 2-second delay between retries
        FixedBackOff backOff = new FixedBackOff(2000L, 3L);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

        // 🌟 CRITICAL FIX: Tell the error handler to commit the offset
        // of the failed record once it is sent to the DLQ.
        errorHandler.setCommitRecovered(true);

        // Don't retry validation errors, send straight to DLQ
        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);

        return errorHandler;
    }

    // 3. Explicit Producer Factory using JsonSerializer
    @Bean
    @Primary
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();

        // Point this to your actual Kafka broker addresses
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, List.of("localhost:9092","localhost:9093", "localhost:9094"));

        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
//        // Define a manual, bulletproof byte-array serializer using Jackson
//        Serializer<Object> jacksonValueSerializer = new Serializer<Object>() {
//            private final ObjectMapper mapper = new ObjectMapper();
//
//            @Override
//            public byte[] serialize(String topic, Object data) {
//                try {
//                    if (data == null) return null;
//                    // Directly write the Java object to clean UTF-8 JSON bytes
//                    return mapper.writeValueAsBytes(data);
//                } catch (Exception e) {
//                    throw new RuntimeException("Error serializing payload to raw JSON bytes", e);
//                }
//            }
//        };
        configProps.put(ProducerConfig.ACKS_CONFIG, "all"); // Ensure all replicas acknowledge for durability
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    // 4. Explicit KafkaTemplate that uses the JSON Factory above
    @Bean(name="customKafka")
    @Primary
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

}
