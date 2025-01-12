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

    public DownloadListener(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Autowired
    private YVCService yvcService;

    @Autowired
    private RabbitResponse rabbitResponse;

    @RabbitListener(queues = "${rabbitmq.download-queue}")
    @Async
    public void handleDownload(VideoDetails videoDetails) throws IOException {
        log.info("開始執行異步下載任務...");

        String command = videoDetails.getMessage();
//        String output = (String) map.get("output");
        log.info("處理下載命令: {}", command);

        Process process = Runtime.getRuntime().exec(command);
        yvcService.getProcessLog(process);

        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("下載失敗，退出碼: " + exitCode);
            }
            log.info("命令執行完成，退出碼: {}", exitCode);
            log.info("下載完成，發送到轉換隊列...");
            rabbitTemplate.convertAndSend("convertQueue", videoDetails);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Process was interrupted", e);
        }
    }
}