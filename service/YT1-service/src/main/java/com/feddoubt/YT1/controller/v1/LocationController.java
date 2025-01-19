package com.feddoubt.YT1.controller.v1;

import com.feddoubt.YT1.config.ConfigProperties;
import com.feddoubt.YT1.redis.RedisIdWorker;
import com.feddoubt.YT1.service.UserLogService;
import com.feddoubt.YT1.service.IpGeolocationService;
import com.feddoubt.YT1.utils.ClientUtils;
import com.feddoubt.common.YT1.config.message.CustomHttpStatus;
import com.feddoubt.common.YT1.config.message.RabbitResponse;
import com.feddoubt.common.YT1.config.message.ResponseUtils;
import com.feddoubt.model.YT1.entity.UserLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/yt1")
public class LocationController {

    private RabbitResponse rabbitResponse;

    public LocationController(RabbitResponse rabbitResponse) {
        this.rabbitResponse = rabbitResponse;
    }

    /**
     * 1. get public ip
     * 2. http://ipinfo.io/{ip}/json ip get loc
     */
    @GetMapping("/init/2")
    public ResponseEntity<?> handleLocationIpinfo() {

        // RabbitTemplate.convertAndSend("userLogQueue", message) 中的 message 为 null，则调用将失败
        // 沒東西要傳，不能直接傳null 隨便傳個字串
        if (!rabbitResponse.queueMessageLog("userLogQueue","null").isSuccess()) {
            return ResponseUtils.httpStatus2ApiResponse(CustomHttpStatus.SERVER_ERROR);
        }
        return ResponseEntity.ok(ResponseUtils.success());
    }

}
