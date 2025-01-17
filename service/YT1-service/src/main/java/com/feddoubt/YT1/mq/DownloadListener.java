package com.feddoubt.YT1.mq;

import com.feddoubt.YT1.service.YVCService;
import com.feddoubt.YT1.utils.ProcessUtils;
import com.feddoubt.common.YT1.config.message.RabbitResponse;
import com.feddoubt.model.YT1.entity.DownloadLog;
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

    private final ProcessUtils processUtils;

    public DownloadListener(ProcessUtils processUtils) {
        this.processUtils = processUtils;
    }

    @RabbitListener(queues = "${rabbitmq.download-queue}")
    @Async
    public void handleDownload(DownloadLog downloadLog) {
        try {
            log.info("異步下載任務前檢查...");
            VideoDetails videoDetails = processUtils.beforeProcessVideoDownload(downloadLog);
            String url = videoDetails.getUrl();
            log.info("開始執行異步下載任務...");
            processUtils.processVideoDownload(url);
        } catch (Exception e) {
            log.error("處理下載任務失敗", e);
        }
    }
}