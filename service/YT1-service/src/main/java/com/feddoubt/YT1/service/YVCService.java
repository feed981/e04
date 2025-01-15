package com.feddoubt.YT1.service;

import com.feddoubt.YT1.config.ConfigProperties;
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

    private final ConfigProperties configProperties;
    private final RabbitTemplate rabbitTemplate;
    private final RabbitResponse rabbitResponse;

    public YVCService(ConfigProperties configProperties ,RabbitTemplate rabbitTemplate ,RabbitResponse rabbitResponse) {
        this.configProperties = configProperties;
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitResponse = rabbitResponse;
    }

    private String ytdlp;
    private String ytdlpContainName;
    private String ffmpegContainName;
    private String yt1baseDir;

    @PostConstruct
    public void init() {
        this.ytdlp = configProperties.getytdlpPath();
        this.ytdlpContainName = configProperties.getytdlpContainName();
        this.yt1baseDir = configProperties.getyt1BaseDir();
        this.ffmpegContainName = configProperties.getffmpegContainName();
    }

    private final static String patternYoutubeUrl = "^https?://(www\\.)?youtube\\.com/watch\\?v=.*$";

    public ResponseEntity<ApiResponse<String>> embedUrl(String url){
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

    // title
    public VideoDetails getVideoTitle(String url) throws IOException, InterruptedException {
        VideoDetails videoDetails = new VideoDetails();
        ProcessBuilder processBuilder = new ProcessBuilder(ytdlp, "--dump-json", url);
        processBuilder.redirectErrorStream(true); // 合併標準錯誤流到標準輸出流
        Process process = processBuilder.start();

        // 讀取命令輸出的標題
        // 讀取命令輸出，指定字符集為 UTF-8
        /**
         * test1: https://www.youtube.com/watch?v=Snv24UgcEoX 不存在的錯誤訪問
         */
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"))) {
            StringBuilder jsonOutput = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if(line.contains("ERROR")){
                    log.info("line: " + line);
                    videoDetails.setMessage(line);
                    return videoDetails;
                }
                jsonOutput.append(line);
            }
            process.waitFor();
//            log.info("Raw output: " + jsonOutput.toString());

            if (jsonOutput.length() == 0) {
                throw new IOException("無法獲取視頻標題。請檢查 URL 或 yt-dlp 命令。");
            }

            // 解析 JSON 輸出
            JSONObject json = new JSONObject(jsonOutput.toString());
            String rawTitle = json.getString("title");
            String ext = json.getString("ext");
            rawTitle = cleanTitle(rawTitle);
            videoDetails.setTitle(rawTitle);
            videoDetails.setExt(ext);

            // 日誌輸出原始標題（可選）
            log.info("rawTitle:{}", rawTitle);

            // 清理標題
            return videoDetails;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }


    //避免這些字符導致文件創建失敗
    private static String cleanTitle(String title) {
        return title.replaceAll("[\\\\/:*?\"<>|]", "_");
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

        VideoDetails videoDetails = getVideoTitle(url);
        videoDetails.setUrl(url);
        String title = videoDetails.getTitle();
        // download youtube video 的原始副檔名
        String ext = videoDetails.getExt();
        downloadLog.setTitle(title);
        downloadLog.setExt(ext);

        // 替換不允許的文件名字符（Windows 文件系統的限制）
        String sanitizedTitle = title.replaceAll("[\\\\/:*?\"<>|.]", "_").replaceAll("\\.\\.", "_");
        downloadLog.setSanitizedTitle(sanitizedTitle);

        videoDetails.setTitle(sanitizedTitle);
        String convertFilename = videoDetails.setConvertFilename();
        String downloadFilename = videoDetails.setDownloadFilename();

        // 1-1. queue: db download log
        ResponseEntity<ApiResponse<String>> downloadLogQueue = rabbitResponse.queueMessageLog("downloadLogQueue", downloadLog);

        if (downloadLogQueue != null) {
            return downloadLogQueue;
        }

        ResponseEntity<ApiResponse<String>> userLogQueue = rabbitResponse.queueMessageLog("userLogQueue", downloadLog.getIpAddress());

        if (userLogQueue != null) {
            return userLogQueue;
        }


        if (title.contains("ERROR")) {
            log.error("title:{}", title);
            return ResponseUtils.httpStatus2ApiResponse(CustomHttpStatus.SERVER_ERROR);
        }

        if (title.isEmpty()) {
            throw new IOException("Failed to fetch video title.");
        }

        // 1-2. queue: frontend embed Url
        ResponseEntity<ApiResponse<String>> embedUrlQueue = embedUrl(url);

        if (embedUrlQueue != null) {
            return embedUrlQueue;
        }

        // 1-3. dto: user need which format mp3 or mp4
        String format = downloadLog.getFormat();
        videoDetails.setFormat(format);


        // 1-4-1. 原始檔案
        String downloadVideoPath = videoDetails.setDownloadVideoPath(yt1baseDir + sanitizedTitle + ext);
        // 1-4-2. 要轉換的格式
        String convertVideoPath = videoDetails.setConvertVideoPath(yt1baseDir + convertFilename);
        log.info("videoDetails:{}",videoDetails);
        log.info("format:{}",format);

        // 發送下載命令
        ResponseEntity<ApiResponse<String>> downloadQueue = rabbitResponse.queueMessageLog("downloadQueue", videoDetails);

        if (downloadQueue != null) {
            return downloadQueue;
        }
        return convertByFormat(videoDetails);
    }


    // 1-5. 原始檔案未下載過
    public ResponseEntity<?> originalFileNotExist(VideoDetails videoDetails) throws IOException {
        String url = videoDetails.getUrl();
        String downloadVideoPath = videoDetails.getDownloadVideoPath();

        if (!new File(downloadVideoPath).exists()) {
            log.info("not download");
            // window
            // String command = String.format("%s -o \"%s\" %s", ytdlp, downloadVideoPath, url);

            return commonProcess(
                    String.format("docker exec %s yt-dlp -o \"%s\" %s", ytdlpContainName, downloadVideoPath, url) ,
                    "轉換","convertQueue" , videoDetails);

        }
        return ResponseEntity.ok(ResponseUtils.success());
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
                ResponseEntity<ApiResponse<String>> notificationQueue = rabbitResponse.queueMessageLog("notificationQueue", convertFilename);
                if (notificationQueue != null) {
                    return notificationQueue;
                }

            } else  {
                log.info("already been downloaded:,{}", downloadVideoPath);
                ResponseEntity<ApiResponse<String>> convertQueue = rabbitResponse.queueMessageLog("convertQueue", videoDetails);
                if (convertQueue != null) {
                    return convertQueue;
                }

            }

        // 1-5-2. mp4: 直接拿原始檔案
        }else if(format.equals("mp4") && new File(downloadVideoPath).exists()){
            log.info("mp4: already been downloaded:,{}", downloadVideoPath);
            ResponseEntity<ApiResponse<String>> notificationQueue = rabbitResponse.queueMessageLog("notificationQueue", downloadFilename);
            if(notificationQueue != null){
                return notificationQueue;
            }
        }
        return ResponseEntity.ok(ResponseUtils.success());
    }

    public ResponseEntity<ApiResponse<String>> convertToMp3(VideoDetails videoDetails){
        return commonProcess(
                String.format(
                        "docker exec %s ffmpeg -i \"%s\" -q:a 0 -map a \"%s\"",
                        ffmpegContainName, videoDetails.getDownloadVideoPath(), videoDetails.getConvertVideoPath()
                ), "前端下載", "notificationQueue", videoDetails.getConvertFilename());
    }

    // ytdlp , ffmepg
    public <T> ResponseEntity<ApiResponse<String>> commonProcess(String dockerCommand ,String logstr,String queue , T t) {

        log.info("dockerCommand:{}",dockerCommand);

        try {
            Process process = Runtime.getRuntime().exec(dockerCommand);
            getProcessLog(process);

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("下載失敗，退出碼: " + exitCode);
            }
            log.info("命令執行完成，退出碼: {}", exitCode);
            log.info(String.format("下載完成，發送到%s隊列...",logstr));
            return rabbitResponse.queueMessageLog(queue, t);

        } catch (InterruptedException | IOException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

    }

    public void getProcessLog(Process process) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info(line);
            }
        } catch (IOException e) {
            throw new RuntimeException("Process was error", e);
        }
    }

    public DownloadFileDetails downloadFile(String filename) throws IOException{
        DownloadFileDetails downloadFileDetails = new DownloadFileDetails();
        // 檔案名稱驗證： 確保檔案名稱中沒有目錄穿越的字元（例如 ../ 或 ..\）
        if (filename.contains("..")) {
            throw new IllegalArgumentException("檔案名稱不合法: " + filename);
        }

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
