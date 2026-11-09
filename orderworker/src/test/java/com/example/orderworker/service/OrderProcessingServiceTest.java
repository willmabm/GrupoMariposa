package com.example.orderworker.service;

import com.example.orderworker.client.EnrichmentClient;
import com.example.orderworker.model.Customer;
import com.example.orderworker.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RedissonReactiveClient;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class OrderProcessingServiceTest {

    @Mock EnrichmentClient enrichmentClient;
    @Mock OrderRepository orderRepository;
    @Mock RetryStoreService retryStoreService;
    @Mock RedissonReactiveClient redisson;
    @Mock ObjectMapper objectMapper;

    @InjectMocks OrderProcessingService service;

    @Test
    void contextLoads() {
        // Prueba vacía solo para verificar que las dependencias inyectadas no fallan
    }
}