package com.deevyanshu.order.service;

import com.deevyanshu.order.Model.Order;
import com.deevyanshu.order.Model.OrderRequest;
import com.deevyanshu.order.Model.OutboxEvent;
import com.deevyanshu.order.Repository.OrderRepository;
import com.deevyanshu.order.Repository.OutboxRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional // Both DB save AND Outbox save happen atomically
    public Order createOrder(OrderRequest request) throws Exception {
        Order order = new Order();
        order.setProductId(request.getProductId());
        order.setAmount(request.getAmount());
        order.setStatus("PENDING");
        order = orderRepository.save(order);

        // Prepare Outbox Event
        OutboxEvent outbox = new OutboxEvent();
        outbox.setAggregateId(order.getId().toString());
        outbox.setTopic("order-created");
        outbox.setPayload(objectMapper.writeValueAsString(order));
        outboxRepository.save(outbox);

        log.info("Order & Outbox saved atomically for ID: {}", order.getId());
        return order;
    }


}
