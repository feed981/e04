package com.feddoubt.YT1.mq;

import com.feddoubt.YT1.redis.RedisIdWorker;
import com.feddoubt.YT1.service.DownloadLogService;
import com.feddoubt.YT1.service.IpGeolocationService;
import com.feddoubt.YT1.service.UserLogService;
import com.feddoubt.YT1.utils.ClientUtils;
import com.feddoubt.common.YT1.config.message.CustomHttpStatus;
import com.feddoubt.common.YT1.config.message.ResponseUtils;
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
    private final IpGeolocationService ipGeolocationService;
    private final RedisIdWorker redisIdWorker;
    private final ClientUtils clientUtils;

    public LogListener(DownloadLogService downloadLogService, UserLogService userLogService, IpGeolocationService ipGeolocationService,
                       RedisIdWorker redisIdWorker, ClientUtils clientUtils) {
        this.downloadLogService = downloadLogService;
        this.userLogService = userLogService;
        this.ipGeolocationService = ipGeolocationService;
        this.redisIdWorker = redisIdWorker;
        this.clientUtils = clientUtils;
    }

    @RabbitListener(queues = "${rabbitmq.user-log-queue}")
    public void handlUserLog(String s) {
        log.info("user-log-queue");
        try {
            String ip = clientUtils.getIp();
            if(ip == null){
                return;
            }
            // add redis find loc no db
//            if(userLogService.findByIpAddress(ip) != null) {
//            }

            UserLog userLog = new UserLog();
            userLog.setIpAddress(ip);
            userLog.setUid(redisIdWorker.nextId("location:" + ip));
            ipGeolocationService.getLocationByIp(userLog);

        } catch (Exception e) {
            log.error("存儲數據庫任務失敗", e);
        }
    }

    @RabbitListener(queues = "${rabbitmq.download-log-queue}")
    public void handleDownloadLog(DownloadLog downloadLog) {
        try {
            String ipAddress = downloadLog.getIpAddress();
            downloadLog.setUserLog(userLogService.findByIpAddress(ipAddress));
            downloadLogService.saveDownloadLog(downloadLog);
        } catch (Exception e) {
            log.error("存儲數據庫任務失敗", e);
        }
    }

}
