package com.feddoubt.common.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // 創建下載隊列，參數：隊列名稱，是否持久化
    @Bean
    public Queue convertQueue() {
        return new Queue("convertQueue", true);
    }

    @Bean
    public Queue downloadLogQueue() { return new Queue("downloadLogQueue", true); }

    @Bean
    public Queue userLogQueue() { return new Queue("userLogQueue", true); }

    @Bean
    public Queue notificationQueue() {
        return new Queue("notificationQueue", true);
    }

    @Bean
    public Queue embedUrlQueue() {
        return new Queue("embedUrlQueue", true);
    }
}
