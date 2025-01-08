package com.feddoubt.YT1.controller.v1;

import com.feddoubt.YT1.service.IYouTubeService;
import com.feddoubt.YT1.service.utils.YouTubeUtils;
import com.feddoubt.YT1.service.redis.DownloadLimiter;
import com.feddoubt.common.YT1.config.message.*;
import com.feddoubt.model.YT1.dtos.YT1Dto;
import com.feddoubt.model.YT1.event.DownloadLogEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
import java.util.Map;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Slf4j
@RestController
@RequestMapping("/api/v1/YT1")
public class YouTubeController {

    @Autowired
    private IYouTubeService youTubeService;

    @Autowired
    private YouTubeUtils youTubeUtils;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private DownloadLimiter downloadLimiter;

    @PostMapping("/convert")
    public ResponseEntity<?> convertToMp3ORMp4(@RequestBody YT1Dto dto , HttpServletRequest request) throws Exception {
        String userIdentifier = getUserIdentifier(request);
        String userAgent = request.getHeader("User-Agent");

//        String ipAddress = Objects.requireNonNull(request.getRemoteAddress()).getAddress().getHostAddress();
//        String userAgent = request.getHeaders().getFirst("User-Agent");
        log.info("ipAddress: {}", userIdentifier);
        log.info("userAgent: {}", userAgent);

        DownloadLogEvent downloadLogEvent = new DownloadLogEvent(userIdentifier, dto.getUrl(), dto.getFormat(), userAgent);
        log.info("Sending event: {}", downloadLogEvent);
        rabbitTemplate.convertAndSend("downloadLogQueue", downloadLogEvent);

        String url = dto.getUrl();
        if (url == null || url.isEmpty()) {
            return ResponseUtils.httpStatus2ApiResponse(CustomHttpStatus.URL_CANNOT_BE_NULL_OR_EMPTY);
        }

        Boolean validYouTubeUrl = youTubeUtils.isValidYouTubeUrl(url);

        if (!validYouTubeUrl) {
            return ResponseUtils.httpStatus2ApiResponse(CustomHttpStatus.INVALID_YOUTUBE_URL);
        }

        return youTubeService.convertToMp3ORMp4(dto);
    }


    @GetMapping("/download")
    public ResponseEntity<?> downloadFile(HttpServletRequest request ,@RequestParam String filename
    ) throws IOException{

        String userIdentifier = getUserIdentifier(request);
        String requestHash = generateRequestHash(userIdentifier + ":" + filename);

        if (!downloadLimiter.tryDownload(requestHash)) {
            return ResponseUtils.httpStatus2ApiResponse(CustomHttpStatus.TOO_MANY_REQUESTS);
        }

        Map<String, Object> stringObjectMap = youTubeService.downloadFile(filename);

        String mimeType = (String) stringObjectMap.get("mimeType");
        String filePath = (String) stringObjectMap.get("Path");

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

    private String getUserIdentifier(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }

    private String generateRequestHash(String... components) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // 将所有组件用 ":" 连接，生成单一字符串
            String data = String.join(":", components);
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to generate hash", e);
        }
    }
}
