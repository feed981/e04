package com.feddoubt.YT1.mq;

import com.feddoubt.YT1.service.YVCService;
import com.feddoubt.model.YT1.pojos.VideoDetails;
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
    private YVCService yvcService;

//    @Autowired
//    private S3Service s3Service;

    private final RabbitTemplate rabbitTemplate;

    public ConvertListener(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = "${rabbitmq.convert-queue}")
    @Async
    public void handleConvert(VideoDetails videoDetails) {
        try {
            log.info("收到轉換任務: {}", videoDetails);
            // 轉換邏輯
            String filename = videoDetails.getDownloadFilename();

            if(videoDetails.getConvertVideoPath().contains("mp3")) {
                filename = yvcService.convertToMp3(videoDetails);
            }

            log.info("notificationQueue success:{}",filename);
            rabbitTemplate.convertAndSend("notificationQueue", filename);
//            if(!result.isEmpty()){
//                s3Service.uploadFileToS3(videoPath);
//            }

        } catch (Exception e) {
            log.error("處理轉換任務失敗", e);
        }
    }

}