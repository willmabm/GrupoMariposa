package com.example.orderworker.service;

import java.time.Duration;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class RetryStoreService {

    private final RedissonReactiveClient redisson;

    public RetryStoreService(RedissonReactiveClient redisson) {
        this.redisson = redisson;
    }

    /** Incrementa el contador de intentos y devuelve el nuevo valor */
    public Mono<Long> incrementAttempt(String orderId) {
        return redisson.getAtomicLong("retry:order:" + orderId)
                .incrementAndGet()
                .timeout(Duration.ofSeconds(2));
    }

    /** Guarda el mensaje fallido (JSON) con TTL para no crecer indefinidamente */
    public Mono<Void> saveFailedMessage(String orderId, String payload) {
        return redisson.getBucket("failed:order:" + orderId)
                .set(payload, Duration.ofHours(24))
                .then();
    }

    /** Recupera el número de intentos actuales (0 si no existe) */
    public Mono<Long> getAttempts(String orderId) {
        return redisson.getAtomicLong("retry:order:" + orderId).get()
                .defaultIfEmpty(0L);
    }
}