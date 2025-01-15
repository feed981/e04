package com.feddoubt.common.YT1.config.message;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RabbitResponse {

    private RabbitTemplate rabbitTemplate;

    public RabbitResponse(RabbitTemplate rabbitTemplate){
        this.rabbitTemplate = rabbitTemplate;
    }

    public <T> ResponseEntity<ApiResponse<String>> queueMessageLog(String queueName, T t){
        try {
            rabbitTemplate.convertAndSend(queueName, t);
            log.info("{} Message sent to the queue successfully.",queueName);
            return null;
        } catch (Exception e) {
            log.error("Failed to send message to the queue: {}", e.getMessage());
            return ResponseUtils.httpStatus2ApiResponse(CustomHttpStatus.SERVER_ERROR);
            // 这里你可以做更多的错误处理，如重试机制或通知系统
        }
    }
}
