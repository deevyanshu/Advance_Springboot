package com.deevyanshu.orderservice.service;

import com.deevyanshu.orderservice.dto.OrderRequest;
import com.deevyanshu.orderservice.event.OrderEvent;
import com.deevyanshu.orderservice.model.Order;
import com.deevyanshu.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    private final WebClient webClient;

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    /**
     * Place an order with resilient inventory stock checking.
     * Handles timeouts, non-2xx responses, and implements fallback strategy.
     *
     * @param orderRequest the order request containing skuCode, quantity, and price
     */
    public void placeOrder(OrderRequest orderRequest) {
        // Check inventory service for stock availability with comprehensive error handling
        Mono<Boolean> isInStock = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/inventory")
                        .queryParam("skuCode", orderRequest.skuCode())
                        .queryParam("quantity", orderRequest.quantity())
                        .build())
                .retrieve()
                // Handle non-2xx status codes
                .onStatus(status -> !status.is2xxSuccessful(), clientResponse -> {
                    int statusCode = clientResponse.statusCode().value();
                    HttpStatus status = HttpStatus.resolve(statusCode);
                    String statusText = (status != null) ? status.getReasonPhrase() : "Unknown Status";
                    log.warn("Inventory service returned non-2xx status: {} {}", statusCode, statusText);

                    // Distinguish between 4xx (client error) and 5xx (server error)
                    if (clientResponse.statusCode().is4xxClientError()) {
                        // Client errors are treated as fatal for this request
                        return Mono.error(new IllegalArgumentException(
                            "Inventory service client error: " + statusCode + " " + statusText));
                    } else {
                        // For server errors (5xx) propagate the response exception so retry/onError handlers can act
                        return clientResponse.createException();
                    }
                })
                .bodyToMono(Boolean.class)
                // Add timeout handling (5 seconds, aligned with WebClientConfig connect timeout)
                .timeout(Duration.ofSeconds(5),
                    Mono.error(new RuntimeException("Inventory service timeout: took longer than 5 seconds")))
                // Retry once on timeout or server errors (idempotent GET request)
                .retryWhen(Retry.backoff(1, Duration.ofMillis(200))
                        .maxBackoff(Duration.ofSeconds(3))          // Maximum delay cap between retries
                        .jitter(0.5)    // Add randomness to prevent "thundering
                        .filter(throwable -> {
                            // Exclude client-side input errors (4xx) from retrying
                            if (throwable instanceof IllegalArgumentException) {
                                return false;
                            }

                            // Retry on 5xx WebClient errors
                            if (throwable instanceof WebClientResponseException ex) {
                                return ex.getStatusCode().is5xxServerError();
                            }

                            // Retry on timeout or connection failures (other RuntimeExceptions)
                            return throwable instanceof RuntimeException;
                        })
                    .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                        log.error("Retries exhausted for inventory check. Applying fallback.",
                            retrySignal.failure());
                        return retrySignal.failure();
                    }))
                // Fallback: if all else fails, reject order to maintain data integrity
                .onErrorResume(throwable -> {
                    if (throwable instanceof RuntimeException && throwable.getMessage().contains("timeout")) {
                        log.error("Inventory service timed out for skuCode={}, quantity={}. Rejecting order.",
                            orderRequest.skuCode(), orderRequest.quantity(), throwable);
                        // this is for handling global exception, you can throw a custom exception here if needed
                        //return Mono.error(new InventoryServiceUnavailableException("Inventory service timed out", throwable));
                    } else if (throwable instanceof WebClientResponseException) {
                        WebClientResponseException ex = (WebClientResponseException) throwable;
                        log.error("Inventory service returned error {} for skuCode={}, quantity={}. Rejecting order.",
                            ex.getStatusCode(), orderRequest.skuCode(), orderRequest.quantity(), ex);
                        // return Mono.error(new InventoryServiceUnavailableException("Inventory service failed", throwable));
                    } else {
                        log.error("Unexpected error checking inventory for skuCode={}, quantity={}. Rejecting order.",
                            orderRequest.skuCode(), orderRequest.quantity(), throwable);
                    }
                    // Return false to indicate stock unavailable (fallback strategy: deny order on service failure)
                    return Mono.just(false);
                });

        // Execute the reactive chain
        isInStock.subscribe(
            isInStockValue -> {
                if (!isInStockValue) {
                    log.warn("Item not in stock: skuCode={}, quantity={}", orderRequest.skuCode(), orderRequest.quantity());
                    throw new IllegalArgumentException("Item not in stock");
                }
                // Proceed with order creation
                Order order = new Order();
                order.setOrderNumber(UUID.randomUUID().toString());
                order.setSkuCode(orderRequest.skuCode());
                order.setQuantity(orderRequest.quantity());
                order.setPrice(orderRequest.price());
                orderRepository.save(order);

                OrderEvent orderEvent=new OrderEvent(orderRequest.orderNumber(),orderRequest.userDetails().email());
                kafkaTemplate.send("order-placed", orderEvent);
                log.info("Order placed successfully: orderNumber={}, skuCode={}", order.getOrderNumber(), order.getSkuCode());
            },
            error -> {
                log.error("Failed to place order: skuCode={}, quantity={}. Error: {}",
                    orderRequest.skuCode(), orderRequest.quantity(), error.getMessage(), error);
                // In a real application, you might want to emit this error to a message queue or notify the client
            },
            () -> log.debug("Order placement completed for skuCode={}", orderRequest.skuCode())
        );
    }

}
