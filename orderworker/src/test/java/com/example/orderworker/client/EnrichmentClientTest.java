package com.example.orderworker.client;

import com.example.orderworker.model.Customer;
import io.github.resilience4j.retry.RetryRegistry;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;

class EnrichmentClientTest {

    private MockWebServer mockWebServer;
    private EnrichmentClient enrichmentClient;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // Construimos la URL base dinámica con el puerto que asignó el MockWebServer
        String baseUrl = mockWebServer.url("/").toString();
        // El MockWebServer incluye una barra al final, la quitamos para evitar dobles barras
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        WebClient webClient = WebClient.builder().build();
        RetryRegistry retryRegistry = RetryRegistry.ofDefaults();

        enrichmentClient = new EnrichmentClient(
                webClient,
                retryRegistry,
                baseUrl
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void shouldFetchCustomerSuccessfully() {
        // Arrange
        String jsonResponse = "{\"customerId\":\"customer-456\",\"name\":\"Ana\",\"active\":true}";
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(jsonResponse));

        // Act & Assert
        StepVerifier.create(enrichmentClient.getCustomer("customer-456"))
                .expectNextMatches(customer -> 
                        customer.getCustomerId().equals("customer-456") && 
                        customer.isActive())
                .verifyComplete();
    }
}