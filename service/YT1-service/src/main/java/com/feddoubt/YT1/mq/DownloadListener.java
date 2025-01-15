package com.feddoubt.YT1.mq;

import com.feddoubt.YT1.service.YVCService;
import com.feddoubt.common.YT1.config.message.RabbitResponse;
import com.feddoubt.model.YT1.pojos.VideoDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
public class DownloadListener {

    private final RabbitTemplate rabbitTemplate;
    private final YVCService yVCService;
    private final RabbitResponse rabbitResponse;

    public DownloadListener(YVCService yVCService ,RabbitTemplate rabbitTemplate,RabbitResponse rabbitResponse) {
        this.yVCService = yVCService;
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitResponse = rabbitResponse;
    }

    @RabbitListener(queues = "${rabbitmq.download-queue}")
    @Async
    public void handleDownload(VideoDetails videoDetails) {
        try {
            log.info("開始執行異步下載任務...");
            yVCService.originalFileNotExist(videoDetails);
        } catch (Exception e) {
            log.error("處理下載任務失敗", e);
        }
    }
}