package com.example.orderworker.service;

import com.example.orderworker.client.EnrichmentClient;
import com.example.orderworker.model.Customer;
import com.example.orderworker.model.OrderDocument;
import com.example.orderworker.model.OrderMessage;
import com.example.orderworker.model.Product;
import com.example.orderworker.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLockReactive;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderProcessingServiceTest {

    @Mock private EnrichmentClient enrichmentClient;
    @Mock private OrderRepository orderRepository;
    @Mock private RetryStoreService retryStoreService;
    @Mock private RedissonReactiveClient redisson;
    @Mock private ObjectMapper objectMapper;
    @Mock private RLockReactive lockReactive;

    @InjectMocks
    private OrderProcessingService service;

    private final String payload = "{\"orderId\":\"order-123\",\"customerId\":\"customer-456\",\"productIds\":[\"product-789\"]}";
    private OrderMessage orderMessage;

    @BeforeEach
    void setUp() {
        // Inyectar valores de @Value que no se cargan en pruebas unitarias
        ReflectionTestUtils.setField(service, "maxAttempts", 3);
        ReflectionTestUtils.setField(service, "initialDelay", Duration.ofMillis(1));

        orderMessage = new OrderMessage();
        orderMessage.setOrderId("order-123");
        orderMessage.setCustomerId("customer-456");
        orderMessage.setProductIds(List.of("product-789"));
    }

    @Test
    void shouldProcessOrderSuccessfully() throws Exception {
        // Arrange
        when(objectMapper.readValue(eq(payload), eq(OrderMessage.class))).thenReturn(orderMessage);
        when(redisson.getLock(anyString())).thenReturn(lockReactive);
        when(lockReactive.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(Mono.just(true));
        
        Customer customer = new Customer();
        customer.setCustomerId("customer-456");
        customer.setActive(true);
        when(enrichmentClient.getCustomer("customer-456")).thenReturn(Mono.just(customer));

        Product product = new Product();
        product.setProductId("product-789");
        product.setName("Laptop");
        when(enrichmentClient.getProduct("product-789")).thenReturn(Mono.just(product));

        when(orderRepository.save(any(OrderDocument.class))).thenReturn(Mono.just(new OrderDocument()));
        when(lockReactive.unlock()).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(service.processOrder(payload))
                .verifyComplete();

        verify(orderRepository, times(1)).save(any(OrderDocument.class));
    }

    @Test
    void shouldFailWhenCustomerIsInactive() throws Exception {
        // Arrange
        when(objectMapper.readValue(eq(payload), eq(OrderMessage.class))).thenReturn(orderMessage);
        when(redisson.getLock(anyString())).thenReturn(lockReactive);
        when(lockReactive.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(Mono.just(true));
        when(lockReactive.unlock()).thenReturn(Mono.empty());

        Customer inactiveCustomer = new Customer();
        inactiveCustomer.setCustomerId("customer-456");
        inactiveCustomer.setActive(false); // Cliente inactivo
        when(enrichmentClient.getCustomer("customer-456")).thenReturn(Mono.just(inactiveCustomer));

        Product product = new Product();
        when(enrichmentClient.getProduct("product-789")).thenReturn(Mono.just(product));

        // Simulamos que falla y entra al manejo de errores
        when(retryStoreService.incrementAttempt(anyString())).thenReturn(Mono.just(3L)); // Llega al maxAttempts
        when(retryStoreService.saveFailedMessage(anyString(), anyString())).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(service.processOrder(payload))
                .verifyComplete(); // Termina porque el error se maneja guardándolo como fallido

        verify(orderRepository, never()).save(any()); // No debe guardar en Mongo
        verify(retryStoreService, times(1)).saveFailedMessage(eq("order-123"), eq(payload));
    }

    @Test
    void shouldNotProcessIfLockIsNotAcquired() throws Exception {
        // Arrange
        when(objectMapper.readValue(eq(payload), eq(OrderMessage.class))).thenReturn(orderMessage);
        when(redisson.getLock(anyString())).thenReturn(lockReactive);
        when(lockReactive.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(Mono.just(false)); // Lock ocupado

        when(retryStoreService.incrementAttempt(anyString())).thenReturn(Mono.just(1L));
        
        // Act & Assert
        StepVerifier.create(service.processOrder(payload))
                .verifyComplete();
                
        verify(orderRepository, never()).save(any());
    }
}