package com.feddoubt.YT1.service;

import com.feddoubt.YT1.config.ConfigProperties;
import com.feddoubt.YT1.utils.ProcessUtils;
import com.feddoubt.common.YT1.config.message.CustomHttpStatus;
import com.feddoubt.common.YT1.config.message.RabbitResponse;
import com.feddoubt.model.YT1.entity.DownloadLog;
import com.feddoubt.model.YT1.pojos.DownloadFileDetails;
import com.feddoubt.model.YT1.pojos.VideoDetails;
import com.feddoubt.common.YT1.config.message.ApiResponse;
import com.feddoubt.common.YT1.config.message.ResponseUtils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
//

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
    private final ProcessUtils processUtils;

    public YVCService(RabbitResponse rabbitResponse ,ProcessUtils processUtils) {
        this.rabbitResponse = rabbitResponse;
        this.processUtils = processUtils;
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
    public ResponseEntity<?> downloadVideoByUrl(DownloadLog downloadLog) throws Exception {
        String url = downloadLog.getUrl();

        if (!rabbitResponse.queueMessageLog("downloadQueue", downloadLog).isSuccess()) {
            return ResponseUtils.httpStatus2ApiResponse(CustomHttpStatus.SERVER_ERROR);
        }

        // 1-1. queue: db download log
        if (!rabbitResponse.queueMessageLog("downloadLogQueue", downloadLog).isSuccess()) {
            return ResponseUtils.httpStatus2ApiResponse(CustomHttpStatus.SERVER_ERROR);
        }

        // 1-2. queue: frontend embed Url
        if (!embedUrl(url).isSuccess()) {
            return ResponseUtils.httpStatus2ApiResponse(CustomHttpStatus.SERVER_ERROR);
        }
        return null;
//        return convertByFormat(videoDetails);
    }


    // 1-5. 原始檔案未下載過
    public ApiResponse<String> originalFileNotExist(VideoDetails videoDetails) throws IOException {
        String url = videoDetails.getUrl();

        if (!new File(downloadVideoPath).exists()) {
            log.info("not download");
            // window
            // String command = String.format("%s -o \"%s\" %s", ytdlp, downloadVideoPath, url);

            return null;
//            return processUtils.commonProcess(
//                    String.format("docker exec %s yt-dlp -o \"%s\" %s", ytdlpContainName, downloadVideoPath, url) ,
//                    "轉換","convertQueue" , videoDetails);

        }
        return ResponseUtils.success();
    }

    public ResponseEntity<?> convertByFormat(VideoDetails videoDetails){
        String format = videoDetails.getFormat();
        String downloadVideoPath = videoDetails.getDownloadVideoPath();
        String downloadFilename = videoDetails.getDownloadFilename();
        String convertVideoPath = videoDetails.getConvertVideoPath();
        String convertFilename = videoDetails.getConvertFilename();
        // 1-5-1. mp3: 检查文件是否已下載並轉換過該格式
        if(format.equals("mp3")){

            if (new File(convertVideoPath).exists()) {
                log.info("file.exists");
                if (!rabbitResponse.queueMessageLog("notificationQueue", convertFilename).isSuccess()) {
                    return ResponseUtils.httpStatus2ApiResponse(CustomHttpStatus.SERVER_ERROR);
                }

            } else  {
                log.info("already been downloaded:,{}", downloadVideoPath);
                if (!rabbitResponse.queueMessageLog("convertQueue", videoDetails).isSuccess()) {
                    return ResponseUtils.httpStatus2ApiResponse(CustomHttpStatus.SERVER_ERROR);
                }

            }

        // 1-5-2. mp4: 直接拿原始檔案
        }else if(format.equals("mp4") && new File(downloadVideoPath).exists()){
            log.info("mp4: already been downloaded:,{}", downloadVideoPath);
            if (!rabbitResponse.queueMessageLog("notificationQueue", downloadFilename).isSuccess()) {
                return ResponseUtils.httpStatus2ApiResponse(CustomHttpStatus.SERVER_ERROR);
            }
        }
        return ResponseEntity.ok(ResponseUtils.success());
    }

    public ApiResponse<String> convertToMp3(VideoDetails videoDetails){
        return null;
//        return processUtils.commonProcess(
//                String.format(
//                        "docker exec %s ffmpeg -i \"%s\" -q:a 0 -map a \"%s\"",
//                        ffmpegContainName, videoDetails.getDownloadVideoPath(), videoDetails.getConvertVideoPath()
//                ), "前端下載", "notificationQueue", videoDetails.getConvertFilename());
    }


    public DownloadFileDetails downloadFile(String filename) throws IOException{
        DownloadFileDetails downloadFileDetails = new DownloadFileDetails();
        // 檔案名稱驗證： 確保檔案名稱中沒有目錄穿越的字元（例如 ../ 或 ..\）
        if (filename.contains("..")) {
            throw new IllegalArgumentException("檔案名稱不合法: " + filename);
        }
String yt1baseDir = null;
        log.info("filepath:{}" , yt1baseDir + filename);

        // 解析並規範化檔案路徑
        Path filePath = Paths.get(yt1baseDir).resolve(filename).normalize();
        downloadFileDetails.setFilename(filename);
        downloadFileDetails.setPath(String.valueOf(filePath));
        log.info("downloadFileDetails:{}",downloadFileDetails);

        // 限制在基礎目錄內： 確保解析後的 filePath 仍位於基礎目錄內
        if (!filePath.startsWith(yt1baseDir)) {
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
