package com.example.orderworker.client;

import com.example.orderworker.model.Customer;
import com.example.orderworker.model.Product;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class EnrichmentClient {

    private final WebClient webClient;
    private final Retry retry;

    public EnrichmentClient(WebClient enrichmentWebClient,
                            RetryRegistry retryRegistry,
                            @Value("${app.enrichment.base-url}") String baseUrl) {
        this.webClient = enrichmentWebClient.mutate().baseUrl(baseUrl).build();
        // Usa la configuración 'default' de Resilience4j de tu application.yml
        this.retry = retryRegistry.retry("default");
    }

    public Mono<Product> getProduct(String productId) {
        return webClient.get()
                .uri("/api/products/{id}", productId)
                .retrieve()
                .bodyToMono(Product.class)
                .transformDeferred(RetryOperator.of(retry));
    }

    public Mono<Customer> getCustomer(String customerId) {
        return webClient.get()
                .uri("/api/customers/{id}", customerId)
                .retrieve()
                .bodyToMono(Customer.class)
                .transformDeferred(RetryOperator.of(retry));
    }
}