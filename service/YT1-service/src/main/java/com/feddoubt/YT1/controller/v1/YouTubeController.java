package com.feddoubt.YT1.controller.v1;

import com.feddoubt.YT1.service.IYouTubeService;
import com.feddoubt.model.YT1.dtos.YT1Dto;
import com.feddoubt.YT1.service.utils.YouTubeUtils;
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;


@Slf4j
@RestController
@RequestMapping("/api/v1/YT1")
public class YouTubeController {

    @Autowired
    private IYouTubeService youTubeService;

    @Autowired
    private YouTubeUtils youTubeUtils;

    @PostMapping("/convert")
    public ResponseEntity<?> convertToMp3ORMp4(@RequestBody YT1Dto dto) {
        String url = dto.getUrl();
        if (url == null || !youTubeUtils.isValidYouTubeUrl(url)) {
            return ResponseEntity.badRequest().body("Invalid YouTube URL");
        }

        try {
            String result = youTubeService.convertToMp3ORMp4(dto);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Conversion failed: " + e.getMessage());
        }
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam String filename, @RequestParam String date) throws IOException{
        try {
            Map<String, Object> map = youTubeService.downloadFile(filename);
            String mimeType = (String) map.get("mimeType");
            String filePath = (String) map.get("Path");
            log.info("mimeType:{}",mimeType);
            log.info("filePath:{}",filePath);
//            String filePath = "C:\\YT1\\download\\" + filename;
            File file = new File(filePath);

            // 創建資源
            Path path = file.toPath();
            Resource resource = new ByteArrayResource(Files.readAllBytes(path));

            // 設置響應頭
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName())
                    .contentType(MediaType.parseMediaType(mimeType))
                    .contentLength(file.length())
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

//    @GetMapping("/downloadtest")
    public ResponseEntity<Resource> downloadFile() {
        try {
            // 模擬 MP3 文件路徑
            String filePath = "C:\\YT1\\download\\Waves.mp3";
            String mimeType = "audio/mpeg";
            File file = new File(filePath);
//            log.info("mimeType:{}",mimeType);
//            log.info("filePath:{}",filePath);
            // 創建資源
            Path path = file.toPath();
            Resource resource = new ByteArrayResource(Files.readAllBytes(path));

            // 設置響應頭
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName())
                    .contentType(MediaType.parseMediaType(mimeType))
                    .contentLength(file.length())
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
