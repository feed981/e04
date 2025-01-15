package com.feddoubt.YT1.controller.v1;

import com.feddoubt.YT1.redis.RedisIdWorker;
import com.feddoubt.YT1.service.IpGeolocationService;
import com.feddoubt.YT1.service.UserLogService;
import com.feddoubt.model.YT1.dtos.LocationInfoDto;
import com.feddoubt.model.YT1.entity.DownloadLog;
import com.feddoubt.model.YT1.entity.UserLog;
import com.feddoubt.model.YT1.pojos.DownloadFileDetails;
import com.feddoubt.YT1.service.YVCService;
import com.feddoubt.YT1.redis.DownloadLimiter;
import com.feddoubt.YT1.utils.HashUtils;
import com.feddoubt.YT1.utils.UserUtils;
import com.feddoubt.common.YT1.config.message.*;
import com.feddoubt.model.YT1.dtos.YT1Dto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/yt1")
public class YouTubeController {

    private final YVCService yVCService;
    private final UserLogService userLogService;
    private final IpGeolocationService ipGeolocationService;
    private final DownloadLimiter downloadLimiter;
    private final UserUtils userUtils;
    private final HashUtils hashUtils;
    private final RedisIdWorker redisIdWorker;
    
    public YouTubeController(YVCService yVCService ,UserLogService userLogService, IpGeolocationService ipGeolocationService,
                             DownloadLimiter downloadLimiter,
                             UserUtils userUtils,HashUtils hashUtils ,
                             RedisIdWorker redisIdWorker) {
        this.yVCService = yVCService;
        this.userLogService = userLogService;
        this.ipGeolocationService = ipGeolocationService;
        this.downloadLimiter = downloadLimiter;
        this.userUtils = userUtils;
        this.hashUtils = hashUtils;
        this.redisIdWorker = redisIdWorker;
    }

    @PostMapping("/convert")
    public ResponseEntity<?> convertToMp3(@RequestBody YT1Dto dto , HttpServletRequest request) throws Exception {
        String url = dto.getUrl();
        if (url == null || url.isEmpty()) {
            return ResponseUtils.httpStatus2ApiResponse(CustomHttpStatus.URL_CANNOT_BE_NULL_OR_EMPTY);
        }
        if (!yVCService.isValidYouTubeUrl(url)) {
            return ResponseUtils.httpStatus2ApiResponse(CustomHttpStatus.INVALID_YOUTUBE_URL);
        }

        String userIdentifier = userUtils.getUserIdentifier(request);
        DownloadLog downloadLog = new DownloadLog();
        downloadLog.setIpAddress(userIdentifier);
        downloadLog.setUserAgent(request.getHeader("User-Agent"));
        downloadLog.setUrl(url);
        downloadLog.setFormat(dto.getFormat());
        downloadLog.setCreatedAt(LocalDateTime.now());
        downloadLog.setUid(redisIdWorker.nextId("convert:" + userIdentifier));
        return yVCService.downloadVideoByUrl(downloadLog);
    }


    @GetMapping("/download")
    public ResponseEntity<?> downloadFile(HttpServletRequest request ,@RequestParam String filename) throws IOException{

        String userIdentifier = userUtils.getUserIdentifier(request);
        String requestHash = hashUtils.generateRequestHash(userIdentifier + ":" + filename);

        log.info("filename:{}",filename);

        if (!downloadLimiter.tryDownload(requestHash)) {
            return ResponseUtils.httpStatus2ApiResponse(CustomHttpStatus.TOO_MANY_REQUESTS);
        }

        DownloadFileDetails downloadFileDetails = yVCService.downloadFile(filename);

        String filePath = downloadFileDetails.getPath();
        String mimeType = downloadFileDetails.getMimeType();

        log.info("mimeType:{}",mimeType);
        log.info("filePath:{}",filePath);
//            String filePath = "C:\\YT1\\download\\" + filename;
        File file = new File(filePath);

        // 創建資源
        Path path = file.toPath();
        Resource resource = null;
        try {
            resource = new ByteArrayResource(Files.readAllBytes(path));
        } catch (IOException e) {
            return ResponseUtils.httpStatus2ApiResponse(new HttpStatusAdapter(HttpStatus.INTERNAL_SERVER_ERROR));
        }

        // 設置響應頭
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName())
                .contentType(MediaType.parseMediaType(mimeType))
                .contentLength(file.length())
                .body(resource);
    }

    @PostMapping("/location")
    public void handleLocation(HttpServletRequest request ,@RequestBody LocationInfoDto locationInfoDto) {
        String userIdentifier = userUtils.getUserIdentifier(request);
        Long byIpAddress = userLogService.findByIpAddress(userIdentifier);
        if(byIpAddress != null){
            log.info("ip exist:{}",byIpAddress);
            return;
        }

        UserLog userLog = new UserLog();
        userLog.setIpAddress(userIdentifier);
        userLog.setUid(redisIdWorker.nextId("location:" + userIdentifier));
        userLog.setCreatedAt(LocalDateTime.now());
        userLog.setLatitude(locationInfoDto.getLatitude());
        userLog.setLongitude(locationInfoDto.getLongitude());
        userLog.setMethod("Geolocation API");
        userLogService.saveUserLog(userLog);
            // method2: ipinfo
//            ipGeolocationService.getLocationByIp(userLog);

    }

}
