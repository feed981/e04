package com.feddoubt.YT1.service.mq;

import com.feddoubt.YT1.repo.DownloadLogRepository;
import com.feddoubt.model.YT1.event.DownloadLogEvent;
import com.feddoubt.model.YT1.entity.DownloadLog;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class DownloadLogListener {

    @Autowired
    private DownloadLogRepository downloadLogRepository;

    @RabbitListener(queues = "${rabbitmq.download-log-queue}")
    @Async
    public void handleDownloadLogEvent(DownloadLogEvent event) {
        DownloadLog log = new DownloadLog(
            event.getIpAddress(),
            event.getUrl(),
            event.getFormat(),
            event.getUserAgent(),
            LocalDateTime.now()
        );
        downloadLogRepository.save(log);
    }
}
