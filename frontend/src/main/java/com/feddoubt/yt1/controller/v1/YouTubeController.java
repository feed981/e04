package com.feddoubt.yt1.controller.v1;

import com.feddoubt.yt1.redis.RedisIdWorker;
import com.feddoubt.yt1.service.IpService;
import com.feddoubt.common.config.message.CustomHttpStatus;
import com.feddoubt.common.config.message.HttpStatusAdapter;
import com.feddoubt.common.config.message.ResponseUtils;
import com.feddoubt.model.entity.DownloadLog;
import com.feddoubt.model.pojos.DownloadFileDetails;
import com.feddoubt.yt1.service.YVCService;
import com.feddoubt.yt1.redis.DownloadLimiter;
import com.feddoubt.yt1.utils.HashUtils;
import com.feddoubt.model.dtos.YT1Dto;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@RestController
@RequestMapping("/api/v1/yt1")
public class YouTubeController {

    private final YVCService yVCService;
    private final DownloadLimiter downloadLimiter;
    private final IpService ipService;
    private final HashUtils hashUtils;
    private final RedisIdWorker redisIdWorker;

    public YouTubeController(YVCService yVCService ,
                             DownloadLimiter downloadLimiter,
                             IpService ipService, HashUtils hashUtils ,
                             RedisIdWorker redisIdWorker) {
        this.yVCService = yVCService;
        this.downloadLimiter = downloadLimiter;
        this.ipService = ipService;
        this.hashUtils = hashUtils;
        this.redisIdWorker = redisIdWorker;
    }

    @GetMapping("/test")
    public ResponseEntity<?> testFilter() {
        log.info("进入 test-filter endpoint");
        return ResponseEntity.ok("Test successful");
    }

    @PostMapping("/convert")
    public ResponseEntity<?> convert(@RequestBody YT1Dto dto , HttpServletRequest request) throws Exception {
        log.info("YouTubeController");
        String url = dto.getUrl();
        if (url == null || url.isEmpty()) {
            return ResponseUtils.httpStatus2ApiResponse(CustomHttpStatus.URL_CANNOT_BE_NULL_OR_EMPTY);
        }
        if (!yVCService.isValidYouTubeUrl(url)) {
            return ResponseUtils.httpStatus2ApiResponse(CustomHttpStatus.INVALID_YOUTUBE_URL);
        }

        String ip = ipService.getClientIp(request);
        DownloadLog downloadLog = new DownloadLog();
        downloadLog.setIpAddress(ip);
        downloadLog.setUserAgent(request.getHeader("User-Agent"));
        downloadLog.setUrl(url);
        downloadLog.setFormat(dto.getFormat());
        downloadLog.setCreatedAt(LocalDateTime.now());
        downloadLog.setUid(redisIdWorker.nextId("convert:" + ip));
        return yVCService.convert(downloadLog);
    }


    @GetMapping("/download")
    public ResponseEntity<?> download(HttpServletRequest request ,@RequestParam String filename) throws IOException{
        String ip = ipService.getClientIp(request);
        String requestHash = hashUtils.generateRequestHash(ip + ":" + filename);

        log.info("filename:{}",filename);

        if (!downloadLimiter.tryDownload(requestHash)) {
            return ResponseUtils.httpStatus2ApiResponse(CustomHttpStatus.TOO_MANY_REQUESTS);
        }

        DownloadFileDetails downloadFileDetails = yVCService.download(filename);

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


}
