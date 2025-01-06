package com.feddoubt.YT1.service.mq;

import com.feddoubt.YT1.service.NotificationService;
import com.feddoubt.YT1.service.utils.YouTubeUtils;
import com.feddoubt.common.YT1.s3.S3Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


import java.util.Map;

@Slf4j
@Service
public class ConvertListener {

    @Autowired
    private YouTubeUtils youTubeUtils;

    @Autowired
    private NotificationService notificationService;

//    @Autowired
//    private S3Service s3Service;

    private final RabbitTemplate rabbitTemplate;

    public ConvertListener(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = "${rabbitmq.convert-queue}")
    @Async
    public void handleConvert(Map<String, Object> map) {
        try {
            log.info("收到轉換任務: {}", map);
            String videoPath = (String) map.get("output");
            log.info("開始轉換: {}", videoPath);

            // 轉換邏輯
            String result = youTubeUtils.convertToMp3ORMp4(map);
            log.info("notificationQueue success:{}",result);
            rabbitTemplate.convertAndSend("notificationQueue", result);
//            if(!result.isEmpty()){
//                s3Service.uploadFileToS3(videoPath);
//            }

        } catch (Exception e) {
            log.error("處理轉換任務失敗", e);
        }
    }

    @RabbitListener(queues = "${rabbitmq.notification-queue}")
    @Async
    public void handleNotification(String message) {
        log.info("message:{}",message);
        // 推送消息到前端
        notificationService.sendNotification("/topic/convert", message);
    }
}