package com.example.orderworker.service;

import com.example.orderworker.client.EnrichmentClient;
import com.example.orderworker.model.Customer;
import com.example.orderworker.model.OrderDocument;
import com.example.orderworker.model.OrderMessage;
import com.example.orderworker.model.Product;
import com.example.orderworker.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLockReactive;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderProcessingService {

    private final EnrichmentClient enrichmentClient;
    private final OrderRepository orderRepository;
    private final RetryStoreService retryStoreService;
    private final RedissonReactiveClient redisson;
    private final ObjectMapper objectMapper;

    @Value("${app.retry.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.retry.initial-delay:500ms}")
    private Duration initialDelay;

    public Mono<Void> processOrder(String payload) {
        return Mono.fromCallable(() -> objectMapper.readValue(payload, OrderMessage.class))
                .flatMap(this::processWithLock)
                .onErrorResume(e -> handleFailure(payload, e))
                .then();
    }

    private Mono<Void> processWithLock(OrderMessage order) {
        RLockReactive lock = redisson.getLock("lock:order:" + order.getOrderId());
        return lock.tryLock(5, 30, TimeUnit.SECONDS)
                .flatMap(acquired -> {
                    if (!acquired) {
                        log.warn("No se pudo adquirir lock para pedido {}", order.getOrderId());
                        return Mono.error(new IllegalStateException("Lock no disponible"));
                    }
                    return doProcess(order)
                            .doFinally(signal -> lock.unlock().subscribe());
                });
    }

    private Mono<Void> doProcess(OrderMessage order) {
        Mono<Customer> customerMono = enrichmentClient.getCustomer(order.getCustomerId());
        Flux<Product> productsFlux = Flux.fromIterable(order.getProductIds())
                .flatMap(enrichmentClient::getProduct);

        return Mono.zip(customerMono, productsFlux.collectList())
                .flatMap(tuple -> {
                    Customer customer = tuple.getT1();
                    List<Product> products = tuple.getT2();

                    if (!customer.isActive()) {
                        return Mono.error(new IllegalArgumentException(
                                "Cliente inactivo: " + customer.getCustomerId()));
                    }
                    if (products.isEmpty() || products.size() != order.getProductIds().size()) {
                        return Mono.error(new IllegalArgumentException(
                                "Productos no encontrados o incompletos"));
                    }

                    OrderDocument doc = new OrderDocument();
                    doc.setOrderId(order.getOrderId());
                    doc.setCustomerId(customer.getCustomerId());
                    doc.setProducts(products);

                    return orderRepository.save(doc).then();
                });
    }

    private Mono<Void> handleFailure(String payload, Throwable error) {
        return Mono.fromCallable(() -> objectMapper.readValue(payload, OrderMessage.class))
                .flatMap(order -> {
                    String orderId = order.getOrderId();
                    return retryStoreService.incrementAttempt(orderId)
                            .flatMap(attempts -> {
                                log.error("Fallo procesando pedido {} (intento {}): {}",
                                        orderId, attempts, error.getMessage());
                                if (attempts >= maxAttempts) {
                                    return retryStoreService.saveFailedMessage(orderId, payload)
                                            .then(Mono.error(error));
                                }
                                long delay = initialDelay.toMillis() * (long) Math.pow(2, attempts - 1);
                                return Mono.delay(Duration.ofMillis(delay))
                                        .then(processOrder(payload));
                            });
                })
                .onErrorResume(e -> Mono.empty())
                .then();
    }
}