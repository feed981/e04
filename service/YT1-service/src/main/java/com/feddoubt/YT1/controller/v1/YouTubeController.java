package com.feddoubt.YT1.controller.v1;

import com.feddoubt.YT1.service.impl.YouTubeService;
import com.feddoubt.model.YT1.dtos.YT1Dto;
import com.feddoubt.model.YT1.dtos.YT1FileDto;
import com.feddoubt.model.YT1.vos.YT1Vo;
import com.feddoubt.utils.YouTubeUtils;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Map;

import static com.hankcs.hanlp.dictionary.CoreBiGramMixDictionary.path;

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

//    @PostMapping("/download")
//    public ResponseEntity<Resource> downloadFile(@RequestBody YT1FileDto dto) throws IOException {
//        try{
//            String filename = dto.getFilename();
//            Map<String, Object> map = youTubeService.downloadFile(filename);
//            String mimeType = (String) map.get("mimeType");
//            String filePath = (String) map.get("Path");
//            File file = new File(filePath);
//
//            // 創建資源
//            Path path = file.toPath();
//            Resource resource = new ByteArrayResource(Files.readAllBytes(path));
//
//            // 設置響應頭
//            return ResponseEntity.ok()
//                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
//                    .contentType(MediaType.parseMediaType(mimeType))
//                    .contentLength(file.length())
//                    .body(resource);
//        } catch (Exception e) {
//            throw new RuntimeException("下載檔案時發生錯誤: " + e.getMessage(), e);
//        }
//    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam String filename, @RequestParam String date) throws IOException{
        try {

            Map<String, Object> map = youTubeService.downloadFile(filename);
            String mimeType = (String) map.get("mimeType");
            String filePath = (String) map.get("Path");

//            String filePath = "C:\\YT1\\download\\" + filename;
            System.out.println("mimeType:" + mimeType);
            System.out.println("filePath:" + filePath);
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

    @GetMapping("/downloadtest")
    public ResponseEntity<Resource> downloadFile() {
        try {
            // 模擬 MP3 文件路徑
            String filePath = "C:\\YT1\\download\\Waves.mp3";
            String mimeType = "audio/mpeg";
            File file = new File(filePath);
            System.out.println("mimeType:" + mimeType);
            System.out.println("filePath:" + filePath);
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
