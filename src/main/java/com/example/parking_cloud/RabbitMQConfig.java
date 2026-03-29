package com.example.parking_cloud;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public Queue parkingQueue() {
        // "true" nghĩa là hàng đợi này sẽ không bị mất khi server khởi động lại
        return new Queue("parking_queue", true);
    }
}