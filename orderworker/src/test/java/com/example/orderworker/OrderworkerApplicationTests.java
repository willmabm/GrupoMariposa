package com.example.orderworker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration," +
    "org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration," +
    "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration," +
    "org.redisson.spring.starter.RedissonAutoConfiguration"
})
class OrderWorkerApplicationTests {

    @Test
    void contextLoads() {
        // Prueba de humo
    }
}