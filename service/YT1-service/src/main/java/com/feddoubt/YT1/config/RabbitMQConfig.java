package com.feddoubt.YT1.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

        // 創建下載隊列，參數：隊列名稱，是否持久化
    @Bean
    public Queue downloadQueue() {
        return new Queue("downloadQueue", true);
    }

    @Bean
    public Queue convertQueue() {
        return new Queue("convertQueue", true);
    }

}

