package com.deevyanshu.order.Controller;

import com.deevyanshu.order.Model.Order;
import com.deevyanshu.order.Model.OrderRequest;
import com.deevyanshu.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody OrderRequest request) throws Exception {
        Order order = orderService.createOrder(request);
        return ResponseEntity.ok(order);
    }
}
