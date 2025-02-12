package com.feddoubt.yt1.mq;

import com.feddoubt.yt1.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FrontListener {

    private final NotificationService notificationService;

    public FrontListener(NotificationService notificationService){
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = "${rabbitmq.embedUrl-queue}")
    public void handleEmbedUrl(String message) {
        try{
            log.info("message:{}",message);
            // 推送消息到前端
            notificationService.sendNotification("/topic/embedUrl", message);
        } catch (Exception e) {
            log.error("推送embedUrl消息到前端任務失敗", e);
        }
    }

    @RabbitListener(queues = "${rabbitmq.notification-queue}")
    public void handleNotification(String message) {
        try{
            log.info("message:{}",message);
            // 推送消息到前端
            notificationService.sendNotification("/topic/convert", message);
        } catch (Exception e) {
            log.error("convert未完成推送可下載消息到前端任務失敗", e);
        }
    }
}
