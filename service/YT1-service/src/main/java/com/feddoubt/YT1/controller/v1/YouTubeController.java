package com.feddoubt.YT1.controller.v1;

import com.feddoubt.YT1.service.IYouTubeService;
import com.feddoubt.common.YT1.dtos.YT1Dto;
import com.feddoubt.YT1.service.utils.YouTubeUtils;
import com.feddoubt.common.YT1.event.DownloadLogEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
//import org.springframework.http.server.reactive.ServerHttpRequest;
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
import java.util.Objects;


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

    @PostMapping("/convert")
    public ResponseEntity<?> convertToMp3ORMp4(@RequestBody YT1Dto dto , HttpServletRequest request) throws Exception {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        String userAgent = request.getHeader("User-Agent");

//        String ipAddress = Objects.requireNonNull(request.getRemoteAddress()).getAddress().getHostAddress();
//        String userAgent = request.getHeaders().getFirst("User-Agent");
        log.info("ipAddress: {}", ipAddress);
        log.info("userAgent: {}", userAgent);

        DownloadLogEvent downloadLogEvent = new DownloadLogEvent(ipAddress, dto.getUrl(), dto.getFormat(), userAgent);
        log.info("Sending event: {}", downloadLogEvent);
        rabbitTemplate.convertAndSend("downloadLogQueue", downloadLogEvent);

        String url = dto.getUrl();
        if (url == null || url.isEmpty()) {
            return ResponseEntity.badRequest().body("URL cannot be null or empty");
        }

        Boolean validYouTubeUrl = youTubeUtils.isValidYouTubeUrl(url);

        if (!validYouTubeUrl) {
            return ResponseEntity.badRequest().body("Invalid YouTube URL");
        }

        String result = youTubeService.convertToMp3ORMp4(dto);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam String filename, @RequestParam String date) throws IOException{
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        // 設置響應頭
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName())
                .contentType(MediaType.parseMediaType(mimeType))
                .contentLength(file.length())
                .body(resource);
    }
}
