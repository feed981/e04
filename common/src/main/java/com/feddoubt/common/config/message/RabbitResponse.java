package com.feddoubt.common.config.message;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RabbitResponse {

    private RabbitTemplate rabbitTemplate;

    public RabbitResponse(RabbitTemplate rabbitTemplate){
        this.rabbitTemplate = rabbitTemplate;
    }

    public <T> ApiResponse<String> queueMessageLog(String queueName, T t){
        try {
            rabbitTemplate.convertAndSend(queueName, t);
            log.info("{} Message sent to the queue successfully.",queueName);
            return ResponseUtils.success();
        } catch (Exception e) {
            log.error("Failed to send message to the queue: {}", e.getMessage());
            return ResponseUtils.error();
        }
    }
}
