package com.feddoubt.YT1.controller.v1;

import com.feddoubt.YT1.redis.RedisIdWorker;
import com.feddoubt.YT1.service.UserLogService;
import com.feddoubt.YT1.service.loc.IpGeolocationService;
import com.feddoubt.YT1.utils.ClientUtils;
import com.feddoubt.common.YT1.config.message.CustomHttpStatus;
import com.feddoubt.common.YT1.config.message.ResponseUtils;
import com.feddoubt.model.YT1.entity.UserLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/yt1")
public class LocationController {

    private final ClientUtils clientUtils;
    private final UserLogService userLogService;
    private final IpGeolocationService ipGeolocationService;
    private final RedisIdWorker redisIdWorker;

    public LocationController(UserLogService userLogService, IpGeolocationService ipGeolocationService,
                               RedisIdWorker redisIdWorker,ClientUtils clientUtils) {
        this.userLogService = userLogService;
        this.ipGeolocationService = ipGeolocationService;
        this.redisIdWorker = redisIdWorker;
        this.clientUtils = clientUtils;
    }

    @GetMapping("/init/2")
    public ResponseEntity<?> handleLocationIpinfo() {
        String ip = clientUtils.getIp();
        if(ip == null){
            return ResponseUtils.httpStatus2ApiResponse(CustomHttpStatus.CONFLICT);
        }

        if(userLogService.findByIpAddress(ip) != null) {
            return ResponseUtils.httpStatus2ApiResponse(CustomHttpStatus.CONFLICT);
        }
        UserLog userLog = new UserLog();
        userLog.setIpAddress(ip);
        userLog.setUid(redisIdWorker.nextId("location:" + ip));
        return ipGeolocationService.getLocationByIp(userLog);
    }

}
