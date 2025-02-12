package com.feddoubt.YT1.service;

import com.feddoubt.YT1.config.ConfigProperties;
import com.feddoubt.common.config.message.CustomHttpStatus;
import com.feddoubt.common.config.message.RabbitResponse;
import com.feddoubt.model.entity.DownloadLog;
import com.feddoubt.model.pojos.DownloadFileDetails;
import com.feddoubt.common.config.message.ApiResponse;
import com.feddoubt.common.config.message.ResponseUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
@Transactional
public class YVCService {

    private final RabbitResponse rabbitResponse;
    private final ConfigProperties configProperties;
    public YVCService(RabbitResponse rabbitResponse  ,ConfigProperties configProperties) {
        this.rabbitResponse = rabbitResponse;
        this.configProperties = configProperties;
    }

    private String yt1BaseDir;

    @PostConstruct
    public void init() {
        this.yt1BaseDir = configProperties.getYt1BaseDir();
    }

    private final static String patternYoutubeUrl = "^https?://(www\\.)?youtube\\.com/watch\\?v=.*$";

    public ApiResponse<String> embedUrl(String url){
        String videoID = "";
        if (url.contains("v=")) {
            int startIndex = url.indexOf("v=") + 2; // 獲取 v= 的位置並加上 2
            int endIndex = url.indexOf("&", startIndex); // 獲取 & 的位置
            if (endIndex == -1) { // 如果沒有 &，則獲取到字符串結尾
                endIndex = url.length();
            }
            videoID = url.substring(startIndex, endIndex);
            return rabbitResponse.queueMessageLog("embedUrlQueue", "https://www.youtube.com/embed/" + videoID);
        }
        return null;
    }

    // 驗證url
    public Boolean isValidYouTubeUrl(String url) {
        return url != null && url.matches(patternYoutubeUrl);
    }

    /**
     * RabbitMQ：适用于 大规模任务处理 和 分布式架构。
     * Executors.newSingleThreadExecutor()：适用于 小规模任务 和 本地处理。
     *
     * convertToMp3 屬於“大任務”
     * 基於以下原因：
     * 資源消耗高：轉換操作需要消耗大量 CPU 和 I/O 資源。
     * 運行時間不確定：處理視頻長短和解析度不同，運行時間可能變化較大。
     * 可能的併發需求：如果需要處理多個視頻，系統負載會迅速增長。
     */
    public ResponseEntity<?> convert(DownloadLog downloadLog) throws Exception {
        String url = downloadLog.getUrl();
        if (!embedUrl(url).isSuccess()) {
            return ResponseUtils.httpStatus2ApiResponse(CustomHttpStatus.SERVER_ERROR);
        }
        if (!rabbitResponse.queueMessageLog("convertQueue", downloadLog).isSuccess()) {
            return ResponseUtils.httpStatus2ApiResponse(CustomHttpStatus.SERVER_ERROR);
        }
        return ResponseEntity.ok(ResponseUtils.success());
    }

    public DownloadFileDetails download(String filename) throws IOException{
        DownloadFileDetails downloadFileDetails = new DownloadFileDetails();
        // 檔案名稱驗證： 確保檔案名稱中沒有目錄穿越的字元（例如 ../ 或 ..\）
        if (filename.contains("..")) {
            throw new IllegalArgumentException("檔案名稱不合法: " + filename);
        }
        log.info("filepath:{}" , yt1BaseDir + filename);

        // 解析並規範化檔案路徑
        Path filePath = Paths.get(yt1BaseDir).resolve(filename).normalize();
        downloadFileDetails.setFilename(filename);
        downloadFileDetails.setPath(String.valueOf(filePath));
        log.info("downloadFileDetails:{}",downloadFileDetails);

        // 限制在基礎目錄內： 確保解析後的 filePath 仍位於基礎目錄內
        if (!filePath.startsWith(yt1BaseDir)) {
            throw new SecurityException("偵測到未授權的存取嘗試。");
        }

        // 檢查檔案是否存在： 在提供檔案之前，檢查檔案是否存在且可讀
        if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
            throw new FileNotFoundException("檔案不存在或無法讀取: " + filePath);
        }

        String mimeType = "application/octet-stream"; // 默認二進制類型
        if (filename.endsWith(".mp3")) {
            mimeType = "audio/mpeg";
        } else if (filename.endsWith(".mp4")) {
            mimeType = "video/mp4";
        }
        downloadFileDetails.setMimeType(mimeType);
        log.info("mimeType:{}",mimeType);

        return downloadFileDetails;
    }

}
