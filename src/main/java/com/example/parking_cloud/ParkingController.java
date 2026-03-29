package com.example.parking_cloud;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ParkingController {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @GetMapping("/vao-cong")
    public String xeVao() {
        String message = "Xe vua vao cong luc: " + java.time.LocalDateTime.now();
        
        // Bắn tin nhắn vào hàng đợi
        rabbitTemplate.convertAndSend("parking_queue", message);
        
        return "🔥 Đã gửi thông báo xe vào tới RabbitMQ!";
    }
}