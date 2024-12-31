package com.feddoubt.YT1.controller.v1;

import com.feddoubt.YT1.service.impl.YouTubeService;
import com.feddoubt.model.YT1.dtos.YT1Dto;
import com.feddoubt.model.YT1.vos.YT1Vo;
import com.feddoubt.utils.YouTubeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/v1/YT1")
public class YouTubeController {

    @Autowired
    private YouTubeService youTubeService;

    // params 包含 { url: 'YouTube URL' }
    @PostMapping("/convert")
    public ResponseEntity<?> convertToMp3(@RequestBody YT1Dto dto) {
        String url = dto.getUrl();
        if (url == null || !YouTubeUtils.isValidYouTubeUrl(url)) {
            return ResponseEntity.badRequest().body("Invalid YouTube URL");
        }

        try {
            YT1Vo yt1Vo = youTubeService.convertToMp3(dto);
            return ResponseEntity.ok(yt1Vo);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Conversion failed: " + e.getMessage());
        }
    }

    @PostMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam("filename") String filename) throws IOException, InterruptedException{
        try{
            Path path = youTubeService.downloadFile(filename);
            // 提供檔案下載
            Resource resource = new UrlResource(path.toUri());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (Exception e) {
            throw new RuntimeException("下載檔案時發生錯誤: " + e.getMessage(), e);
        }
    }
}
