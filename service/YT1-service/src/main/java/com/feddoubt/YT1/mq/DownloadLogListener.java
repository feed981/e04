package com.feddoubt.YT1.mq;

import com.feddoubt.YT1.service.YVCService;
import com.feddoubt.model.YT1.entity.DownloadLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DownloadLogListener {

    @Autowired
    private YVCService yvcService;

    @RabbitListener(queues = "${rabbitmq.download-log-queue}")
    @Async
    public void handleDownloadLog(DownloadLog downloadLog) {
        log.info("downloadLog:{}",downloadLog);
        yvcService.save(downloadLog);
    }
}
