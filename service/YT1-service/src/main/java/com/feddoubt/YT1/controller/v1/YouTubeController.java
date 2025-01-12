package com.feddoubt.YT1.controller.v1;

import com.feddoubt.model.YT1.entity.DownloadLog;
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

@Slf4j
@RestController
@RequestMapping("/api/v1/YT1")
public class YouTubeController {

    @Autowired
    private YVCService yvcService;

    @Autowired
    private DownloadLimiter downloadLimiter;

    @Autowired
    private UserUtils userUtils;

    @Autowired
    private HashUtils hashUtils;

    @PostMapping("/convert")
    public ResponseEntity<?> convertToMp3(@RequestBody YT1Dto dto , HttpServletRequest request) throws Exception {
        String url = dto.getUrl();
        if (url == null || url.isEmpty()) {
            return ResponseUtils.httpStatus2ApiResponse(CustomHttpStatus.URL_CANNOT_BE_NULL_OR_EMPTY);
        }
        if (!yvcService.isValidYouTubeUrl(url)) {
            return ResponseUtils.httpStatus2ApiResponse(CustomHttpStatus.INVALID_YOUTUBE_URL);
        }

        DownloadLog downloadLog = new DownloadLog();
        downloadLog.setIpAddress(userUtils.getUserIdentifier(request));
        downloadLog.setUserAgent(request.getHeader("User-Agent"));
        downloadLog.setUrl(url);
        downloadLog.setFormat(dto.getFormat());
        downloadLog.setCreatedAt(LocalDateTime.now());
        return yvcService.downloadVideoByUrl(downloadLog);
    }


    @GetMapping("/download")
    public ResponseEntity<?> downloadFile(HttpServletRequest request ,@RequestParam String filename) throws IOException{

        String userIdentifier = userUtils.getUserIdentifier(request);
        String requestHash = hashUtils.generateRequestHash(userIdentifier + ":" + filename);

        log.info("filename:{}",filename);

        if (!downloadLimiter.tryDownload(requestHash)) {
            return ResponseUtils.httpStatus2ApiResponse(CustomHttpStatus.TOO_MANY_REQUESTS);
        }

        DownloadFileDetails downloadFileDetails = yvcService.downloadFile(filename);

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
