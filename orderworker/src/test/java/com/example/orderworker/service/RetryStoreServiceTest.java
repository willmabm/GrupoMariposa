package com.example.orderworker.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RAtomicLongReactive;
import org.redisson.api.RBucketReactive;
import org.redisson.api.RedissonReactiveClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetryStoreServiceTest {

    @Mock private RedissonReactiveClient redisson;
    @Mock private RAtomicLongReactive atomicLong;
    @Mock private RBucketReactive<Object> bucket;

    @InjectMocks
    private RetryStoreService retryStoreService;

    @Test
    void shouldIncrementAttempt() {
        when(redisson.getAtomicLong("retry:order:order-123")).thenReturn(atomicLong);
        when(atomicLong.incrementAndGet()).thenReturn(Mono.just(2L));

        StepVerifier.create(retryStoreService.incrementAttempt("order-123"))
                .expectNext(2L)
                .verifyComplete();
    }

    @Test
    void shouldSaveFailedMessageWithTtl() {
        when(redisson.getBucket("failed:order:order-123")).thenReturn(bucket);
        when(bucket.set(eq("payload"), any(Duration.class))).thenReturn(Mono.empty());

        StepVerifier.create(retryStoreService.saveFailedMessage("order-123", "payload"))
                .verifyComplete();

        verify(bucket, times(1)).set(eq("payload"), any(Duration.class));
    }
}