package com.feddoubt.YT1.service.mq;

import com.feddoubt.YT1.service.utils.YouTubeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
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
    private YouTubeUtils youTubeUtils;

    @RabbitListener(queues = "${rabbitmq.download-queue}")
    @Async
    public void handleDownload(Map<String, Object> map) throws IOException {
        log.info("開始執行異步下載任務...");

        String command = (String) map.get("command");
//        String output = (String) map.get("output");
        log.info("處理下載命令: {}", command);

        Process process = Runtime.getRuntime().exec(command);
        youTubeUtils.getProcessLog(process);

        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("下載失敗，退出碼: " + exitCode);
            }
            log.info("命令執行完成，退出碼: {}", exitCode);
            log.info("下載完成，發送到轉換隊列...");
            rabbitTemplate.convertAndSend("convertQueue", map);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Process was interrupted", e);
        }
    }
}