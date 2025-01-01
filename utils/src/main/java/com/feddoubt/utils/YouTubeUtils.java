package com.feddoubt.utils;

import com.feddoubt.common.ws.ProgressWebSocketServer;
import com.feddoubt.model.YT1.dtos.YT1Dto;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class YouTubeUtils {

    private static final Logger logger = LoggerFactory.getLogger(YouTubeUtils.class);

    private static final String ytdlp = "C:\\Tools\\yt-dlp\\yt-dlp.exe";
    private static final String outputDirectory = "C:\\YT1\\download\\";
    private static final String videoPathtDirectory = outputDirectory + "output\\";

    private static final String YT1baseDir = "C:\\YT1\\download\\";

    @Autowired
    private ProgressWebSocketServer progressWebSocketServer;

    // 驗證url
    public boolean isValidYouTubeUrl(String url) {
        return url != null && url.matches("^https?://(www\\.)?youtube\\.com/watch\\?v=.*$");
    }

    // title
    public String getVideoTitle(String url) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(ytdlp, "--dump-json", url);
        processBuilder.redirectErrorStream(true); // 合併標準錯誤流到標準輸出流
        Process process = processBuilder.start();

        // 讀取命令輸出的標題
        // 讀取命令輸出，指定字符集為 UTF-8
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"))) {
            StringBuilder jsonOutput = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonOutput.append(line);
            }
            process.waitFor();
            if (jsonOutput.length() == 0) {
                throw new IOException("無法獲取視頻標題。請檢查 URL 或 yt-dlp 命令。");
            }

            // 解析 JSON 輸出
            JSONObject json = new JSONObject(jsonOutput.toString());
            String rawTitle = json.getString("title");

            // 日誌輸出原始標題（可選）
            logger.info("rawTitle:{}", rawTitle);

            // 清理標題
            return cleanTitle(rawTitle);
//            return null;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    //避免這些字符導致文件創建失敗
    private static String cleanTitle(String title) {
        return title.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();


    // download youtube video
    public Map<String, Object> downloadVideo(YT1Dto dto) throws IOException, InterruptedException {
        String url = dto.getUrl();
        String format = dto.getFormat();//mp3 ,mp4
        Map<String, Object> map = new HashMap<>();
        map.put("format" ,format);
        logger.info("format:{}", format);

        map.put("result" ,"ok");
        String title = getVideoTitle(url);
        String filename = title + "." + format;
        map.put("filename" ,filename);

        String outputPath = outputDirectory + "\\" + filename;
        File file = new File(outputPath);
        // 检查文件是否存在
        if(file.exists()){
            map.put("result" ,"exist");
            return map;
        }
        map.put("title" ,title);
        logger.info("title:{}", title);
        if (title == null || title.isEmpty()) {
            throw new IOException("Failed to fetch video title.");
        }

        // 替換不允許的文件名字符（Windows 文件系統的限制）
        String sanitizedTitle = title.replaceAll("[\\\\/:*?\"<>|]", "_");
        String output = videoPathtDirectory + sanitizedTitle + ".mp4.webm";

        map.put("output", output);
        logger.info("output:{}", output);

        // 發送下載命令
        String command = String.format("%s -o \"%s\" %s",ytdlp, output, url);
        Process process = Runtime.getRuntime().exec(command);

        // 異步執行下載後的 convertToMp3 操作，避免阻塞主線程
        EXECUTOR.submit(() -> {
            try {
                // 等待進程完成
                double duration = duration(process);
                logger.info("duration:{}", duration);
                process.waitFor();
                map.put("duration", duration);

                // 異步執行 MP3 轉換
                convertToMp3ORMp4(map);
                logger.info("MP3 conversion completed for: {}", output);
                // 您可以在這裡觸發完成通知（例如 WebSocket 消息或數據庫更新）
            } catch (Exception e) {
                logger.error("Error during video download or MP3 conversion", e);
            }
        });

        return map;
    }

    // 解析 yt-dlp 輸出的流， 搜尋總時長
    private double duration(Process process) throws IOException, InterruptedException {
        double totalSeconds = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            String duration = null;

            while ((line = reader.readLine()) != null) {
                logger.info(line); // 查看輸出，方便調試

                // 搜尋總時長（例如: Duration: 00:05:23）
                if (line.contains("Duration")) {
                    duration = line.substring(line.indexOf("Duration:") + 9).trim().split(",")[0];
                    logger.info("Total Duration:{}",duration);
                }
            }

            // 如果需要，可以將時長轉為秒數方便計算進度
            if (duration != null) {
                totalSeconds = parseTimeToSeconds(duration);
                logger.info("Total Duration in Seconds:{}", totalSeconds);
            }
        }
        return totalSeconds;
    }

    // 秒
    private double parseTimeToSeconds(String time) {
        String[] parts = time.split(":");
        double hours = Double.parseDouble(parts[0]);
        double minutes = Double.parseDouble(parts[1]);
        double seconds = Double.parseDouble(parts[2]);
        return hours * 3600 + minutes * 60 + seconds;
    }

    // 基於總時長計算出實時進度百分比
    private void getTimeInfo(String line ,int totalSeconds){
        // 使用正則表達式提取 time= 後的時間部分
        Pattern pattern = Pattern.compile("time=(\\d{2}:\\d{2}:\\d{2}\\.\\d{2})");
        Matcher matcher = pattern.matcher(line);

        if (matcher.find()) {
            String timeInfo = matcher.group(1); // 提取的時間部分
            logger.info("Current Progress:{}",timeInfo);

            // 計算進度百分比並推送
            double currentSeconds = parseTimeToSeconds(timeInfo);
            double progress = ((double) currentSeconds / totalSeconds) * 100;
            int progressPercentage = (int) progress;
            logger.info("progressPercentage:{}",progressPercentage);
            // 推送進度到前端
//            ProgressWebSocketServer.sendProgress(progressPercentage);
        }
    }

    // format: mp3 ,mp4
    public void convertToMp3ORMp4(Map<String, Object> map) throws IOException, InterruptedException {
        String outputPath = outputDirectory + (String) map.get("filename");
        String videoPath = (String) map.get("output");
        int totalSeconds = (int) map.get("duration");

        // 拼接输出路径
        logger.info("convertToMp3ORMp4 videoPath:{}", videoPath);
        logger.info("convertToMp3ORMp4 outputPath:{}", outputPath);

        Process process = getProcess(videoPath, outputPath);

        // 读取输出流并解析进度
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                if (line.contains("time=")) {
                    // 提取时间信息（例如 time=00:00:10.50）
                    String timeInfo = line.substring(line.indexOf("time=") + 5, line.indexOf("bitrate=")).trim();
                    System.out.println("Current Progress: " + timeInfo);
                    getTimeInfo(line ,totalSeconds);
                }
            }
        }

        // 等待进程完成
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Conversion failed with exit code " + exitCode);
        }
    }

    private Process getProcess(String videoPath, String outputPath) throws IOException {
        File videoFile = new File(videoPath);
        if (!videoFile.exists()) {
            throw new FileNotFoundException("Input video file does not exist: " + videoPath);
        }

        // 确保路径中的目录存在
        new File(outputDirectory).mkdirs();

        // 创建命令
        String command = String.format("ffmpeg -i \"%s\" -q:a 0 -map a \"%s\"", videoPath, outputPath);

        // 使用 ProcessBuilder 执行命令
        ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command);
        builder.redirectErrorStream(true); // 将错误流合并到标准输出流
        return builder.start();
    }

    public Map<String, Object> downloadFileYT1(String filename) throws IOException {
        Map<String, Object> map = new HashMap<>(2);
        // 檔案名稱驗證： 確保檔案名稱中沒有目錄穿越的字元（例如 ../ 或 ..\）
        if (filename.contains("..")) {
            throw new IllegalArgumentException("檔案名稱不合法: " + filename);
        }

        // 解析並規範化檔案路徑
        Path filePath = Paths.get(YT1baseDir).resolve(filename).normalize();
        map.put("Path",String.valueOf(filePath));
        logger.info("filePath:{}",filePath);

        // 限制在基礎目錄內： 確保解析後的 filePath 仍位於基礎目錄內
        if (!filePath.startsWith(YT1baseDir)) {
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
        map.put("mimeType",mimeType);
        logger.info("mimeType:{}",mimeType);

        return map;
    }
}
