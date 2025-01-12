package com.feddoubt.YT1.mq;

import com.feddoubt.YT1.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FrontListener {

    @Autowired
    private NotificationService notificationService;

    @RabbitListener(queues = "${rabbitmq.embedUrl-queue}")
    @Async
    public void handleEmbedUrl(String message) {
        log.info("message:{}",message);
        // 推送消息到前端
        notificationService.sendNotification("/topic/embedUrl", message);
    }

    @RabbitListener(queues = "${rabbitmq.notification-queue}")
    @Async
    public void handleNotification(String message) {
        log.info("message:{}",message);
        // 推送消息到前端
        notificationService.sendNotification("/topic/convert", message);
    }
}
