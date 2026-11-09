package com.example.orderworker.consumer;

import com.example.orderworker.service.OrderProcessingService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderKafkaConsumer {

    private final ReceiverOptions<String, String> receiverOptions;
    private final OrderProcessingService processingService;

    @PostConstruct
    public void start() {
        KafkaReceiver.create(receiverOptions)
                .receive()
                .concatMap(record -> {
                    String payload = record.value();
                    log.info("Mensaje recibido offset={} partition={}",
                            record.offset(), record.partition());
                    return processingService.processOrder(payload)
                            .doOnSuccess(v -> record.receiverOffset().acknowledge())
                            .doOnError(e -> log.error("Error procesando mensaje", e))
                            .onErrorResume(e -> {
                                // Ack manual para no bloquear la partición
                                record.receiverOffset().acknowledge();
                                return reactor.core.publisher.Mono.empty();
                            });
                })
                .subscribe();
    }
}