package com.feddoubt.YT1.controller.v1;

import com.feddoubt.YT1.service.impl.YouTubeService;
import com.feddoubt.model.YT1.dtos.YT1Dto;
import com.feddoubt.utils.YouTubeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/YT1")
public class YouTubeController {

    @Autowired
    private YouTubeService youTubeService;
    @PostMapping("/hello")
    public ResponseEntity<?> hello(@RequestBody YT1Dto dto) {
        String url = dto.getUrl();
        return ResponseEntity.ok(url);
    }

    // params 包含 { url: 'YouTube URL' }
    @PostMapping("/convert")
    public ResponseEntity<?> convertToMp3(@RequestBody YT1Dto dto) {
        String url = dto.getUrl();
        if (url == null || !YouTubeUtils.isValidYouTubeUrl(url)) {
            return ResponseEntity.badRequest().body("Invalid YouTube URL");
        }

        try {
            String filePath = youTubeService.convertToMp3(url);
            return ResponseEntity.ok(Map.of("filePath", filePath));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Conversion failed: " + e.getMessage());
        }
    }
}
