package com.feddoubt.YT1.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
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
    public void handleDownload(Map<String, Object> map) {
        log.info("開始執行異步下載任務...");
        try {
            String command = (String) map.get("command");
            log.info("處理下載命令: {}", command);

            Process process = Runtime.getRuntime().exec(command);
            youTubeUtils.getProcessLog(process);
            int exitCode = process.waitFor();
            log.info("命令執行完成，退出碼: {}", exitCode);

            if (exitCode != 0) {
                throw new IOException("下載失敗，退出碼: " + exitCode);
            }

            log.info("下載完成，發送到轉換隊列...");
            rabbitTemplate.convertAndSend("convertQueue", map);
        } catch (Exception e) {
            log.error("處理下載命令失敗", e);
        }
    }
}