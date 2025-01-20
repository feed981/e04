package com.feddoubt.YT1.mq;

import com.feddoubt.YT1.redis.RedisIdWorker;
import com.feddoubt.YT1.service.DownloadLogService;
import com.feddoubt.YT1.service.IpService;
import com.feddoubt.YT1.service.UserLogService;
import com.feddoubt.model.YT1.entity.DownloadLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@Service
public class LogListener {

    private final DownloadLogService downloadLogService;
    private final UserLogService userLogService;
    private final IpService ipService;
    private final RedisIdWorker redisIdWorker;

    public LogListener(DownloadLogService downloadLogService, UserLogService userLogService, IpService ipService,
                       RedisIdWorker redisIdWorker) {
        this.downloadLogService = downloadLogService;
        this.userLogService = userLogService;
        this.ipService = ipService;
        this.redisIdWorker = redisIdWorker;
    }

    @RabbitListener(queues = "${rabbitmq.user-log-queue}")
    public void handlUserLog(HttpServletRequest request) {
        try {
            log.info("開始取得當前公網IP...");
            String ipForUser = ipService.getIpForUser();
            if(ipForUser == null){
                ipForUser = ipService.getClientIp(request);
            }
            log.info("當前公網IP:{}",ipForUser);
            if(ipForUser == null) {
                log.info("未取得公網IP...");
                return;
            }

        } catch (Exception e) {
            log.error("公網IP任務失敗...", e);
        }

        try {
            log.info("開始取得當前公網IP loc...");
            String redisLocation = ipService.getRedisLocation();
            if(redisLocation == null){
                log.info("未取得公網IP loc...");
                ipService.saveUserLog();
            }

        } catch (Exception e) {
            log.error("公網IP loc任務失敗...", e);
        }
    }

    @RabbitListener(queues = "${rabbitmq.download-log-queue}")
    public void handleDownloadLog(DownloadLog downloadLog) {
        try {
            String ipAddress = downloadLog.getIpAddress();
            downloadLog.setUserLog(userLogService.findByIpAddress(ipAddress));
            downloadLogService.saveDownloadLog(downloadLog);
        } catch (Exception e) {
            log.error("存儲數據庫任務失敗...", e);
        }
    }

}
