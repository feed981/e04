package com.feddoubt.YT1.mq;

import com.feddoubt.YT1.service.DownloadLogService;
import com.feddoubt.YT1.service.UserLogService;
import com.feddoubt.model.YT1.entity.DownloadLog;
import com.feddoubt.model.YT1.entity.UserLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LogListener {

    private final DownloadLogService downloadLogService;
    private final UserLogService userLogService;

    public LogListener(DownloadLogService downloadLogService, UserLogService userLogService) {
        this.downloadLogService = downloadLogService;
        this.userLogService = userLogService;
    }

    @RabbitListener(queues = "${rabbitmq.download-log-queue}")
    @Async
    public void handleDownloadLog(DownloadLog downloadLog) {
        try {
            String ipAddress = downloadLog.getIpAddress();
            Long byIpAddress = userLogService.findByIpAddress(ipAddress);
            UserLog userLog = new UserLog();
            userLog.setId(byIpAddress);
            downloadLog.setUserLog(userLog);
            downloadLogService.saveDownloadLog(downloadLog);
        } catch (Exception e) {
            log.error("存儲數據庫任務失敗", e);
        }
    }

}
