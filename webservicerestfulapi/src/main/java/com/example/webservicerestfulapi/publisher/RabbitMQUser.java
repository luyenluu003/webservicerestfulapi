package com.example.webservicerestfulapi.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RabbitMQUser {
    @Value("webservicerestfulapi_exchange")
    private String exchange;

    @Value("webservicerestfulapi_routing_key")
    private String routingKey;

    private RabbitTemplate rabbitTemplate;

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQUser.class);

    public RabbitMQUser(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendMessage(String message) {
        LOGGER.info(String.format("Message sent: %s", message));
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }
}
