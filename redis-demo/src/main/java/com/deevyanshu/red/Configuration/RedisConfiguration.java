package com.deevyanshu.red.Configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfiguration {

    // It defines how your Spring application connects to Redis and how it reads/writes data.
    // The RedisConnectionFactory bean establishes the connection to the Redis server.
    // The RedisTemplate bean provides a high-level abstraction for interacting with Redis, allowing you to perform operations like saving and retrieving data.
    // The StringRedisSerializer is used to serialize keys as strings, while the GenericJackson2JsonRedisSerializer is used to serialize values as JSON.
    // This configuration is essential for enabling Redis caching and data storage in your Spring application.
    // Lettuce is a popular, modern, and asynchronous Redis client library for Java. By default, it manages a pool of connections and allows multiple threads to share the same connection safely, making it highly efficient.
    // Redis doesn't understand Java objects; it only understands raw bytes. Serializers act as translators between Java and Redis.
    // Key Serializer: This ensures that your Java String keys are saved in Redis as clean, readable text strings. Without this, Spring might prepend weird binary characters to your keys in Redis (e.g., \xac\xed\x00\x05t\x00\x04mykey).
    // Value Serializer: This translates your Java Object values into a JSON string before saving them to Redis, and converts them back to Java objects when you read them.

    @Bean
    public RedisConnectionFactory connectionFactory()
    {
        return new LettuceConnectionFactory();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        // Configure the template as needed (e.g., set serializers)
        template.setConnectionFactory(connectionFactory());
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}
