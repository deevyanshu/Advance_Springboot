package com.deevyanshu.red.model;

import java.io.Serializable;

// When a Java class implements Serializable, it flags to the JVM that objects of this class can be converted into a sequence of bytes.
//
//This byte stream can then be saved to a file, sent over a network, or—most importantly for Spring Boot applications—stored in a cache like Redis or an HTTP Session.
//Why do we need it in Spring Boot?
//
//While you don't need Serializable for basic REST APIs that return JSON (because libraries like Jackson convert Java objects to JSON text), you absolutely need it in the following Spring Boot scenarios:
//1. Caching (e.g., Redis, Ehcache)
//
//When you use Spring's @Cacheable annotation to store a Java object in Redis, Spring Data Redis needs a way to turn your Java object into a byte array to ship it over the network to the Redis server. If your Model/DTO class doesn't implement Serializable, Spring will throw a NotSerializableException.
//2. Distributed Sessions
//
//If you run multiple instances of your Spring Boot app behind a load balancer and use Spring Session to store user sessions in a database or Redis, every object you put in the user's session must be Serializable.
//3. Message Brokers (e.g., RabbitMQ, ActiveMQ)
//
//When sending Java objects as messages across a queue using JmsTemplate or RabbitTemplate, the objects often need to be serialized into bytes to travel across the network wire.
public class User implements Serializable {

    private String userId;

    private String name;

    private String phone;

    private String email;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public User() {
    }

    public User(String userId, String name, String phone, String email) {
        this.userId = userId;
        this.name = name;
        this.phone = phone;
        this.email = email;
    }
}
