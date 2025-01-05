package com.feddoubt.YT1.service.utils;

import com.feddoubt.YT1.config.ConfigProperties;
import com.feddoubt.common.YT1.dtos.YT1Dto;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.io.*;
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

    @Autowired
    private ConfigProperties configProperties;

    private String ytdlp;
    private String YT1baseDir;

    @PostConstruct
    public void init() {
        this.ytdlp = configProperties.getYtdlpPath();
        this.YT1baseDir = configProperties.getYt1BaseDir();
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // 驗證url
    public Mono<Boolean> isValidYouTubeUrl(String url) {
        return Mono.just(url != null && url.matches("^https?://(www\\.)?youtube\\.com/watch\\?v=.*$"));
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
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    //避免這些字符導致文件創建失敗
    private static String cleanTitle(String title) {
        return title.replaceAll("[\\\\/:*?\"<>|]", "_");
    }


    // download youtube video

    /**
     *
     * RabbitMQ：适用于 大规模任务处理 和 分布式架构。
     * Executors.newSingleThreadExecutor()：适用于 小规模任务 和 本地处理。
     *
     * convertToMp3ORMp4 屬於“大任務”
     * 基於以下原因：
     * 資源消耗高：轉換操作需要消耗大量 CPU 和 I/O 資源。
     * 運行時間不確定：處理視頻長短和解析度不同，運行時間可能變化較大。
     * 可能的併發需求：如果需要處理多個視頻，系統負載會迅速增長。
     */
    public Mono<String> downloadVideo(YT1Dto dto) throws IOException, InterruptedException {
//        logger.info("ytdlp:{}", ytdlp);
//        logger.info("YT1baseDir:{}", YT1baseDir);
        String url = dto.getUrl();
        String format = dto.getFormat();//mp3 ,mp4
        Map<String, Object> map = new HashMap<>();
        map.put("format" ,format);
        logger.info("format:{}", format);

        String title = getVideoTitle(url);
        String filename = title + "." + format;
        map.put("filename" ,filename);

        logger.info("title:{}", title);
        if (title == null || title.isEmpty()) {
            throw new IOException("Failed to fetch video title.");
        }

        // 替換不允許的文件名字符（Windows 文件系統的限制）
        String sanitizedTitle = title.replaceAll("[\\\\/:*?\"<>|]", "_");
        String output = YT1baseDir + "output\\" + sanitizedTitle + ".mp4.webm";

        File file = new File(output);
        // 检查文件是否存在
        if(file.exists()){
            return Mono.just("exist");
        }

        map.put("output", output);
        logger.info("output:{}", output);

        // 發送下載命令
        String command = String.format("%s -o \"%s\" %s",ytdlp, output, url);
        map.put("command",command);
        rabbitTemplate.convertAndSend("downloadQueue", map);

        return Mono.just("ok");
    }

    // 解析 yt-dlp 輸出的流， 搜尋總時長
//    public double duration(Process process) throws IOException, InterruptedException {
//        double totalSeconds = 0;
//        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
//            String line;
//            String duration = null;
//
//            while ((line = reader.readLine()) != null) {
//                logger.info(line); // 查看輸出，方便調試
//
//                // 搜尋總時長（例如: Duration: 00:05:23）
//                if (line.contains("Duration")) {
//                    duration = line.substring(line.indexOf("Duration:") + 9).trim().split(",")[0];
//                    logger.info("Total Duration:{}",duration);
//                }
//            }
//
//            // 如果需要，可以將時長轉為秒數方便計算進度
//            if (duration != null) {
//                totalSeconds = parseTimeToSeconds(duration);
//                logger.info("Total Duration in Seconds:{}", totalSeconds);
//            }
//        }
//        return totalSeconds;
//    }

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
    public Mono<Void> convertToMp3ORMp4(Map<String, Object> map) throws IOException {
        String outputPath = YT1baseDir + map.get("filename");
        String videoPath = (String) map.get("output");

        logger.info("convertToMp3ORMp4 videoPath: {}", videoPath);
        logger.info("convertToMp3ORMp4 outputPath: {}", outputPath);

        return getProcess(videoPath, outputPath)
                .flatMap(this::getProcessLog) // 打印日志后返回 Process
                .flatMap(process -> Mono.fromCallable(() -> {
                    try {
                        int exitCode = process.waitFor();
                        if (exitCode != 0) {
                            throw new IOException("Conversion failed with exit code " + exitCode);
                        }
                        return null;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Process was interrupted", e);
                    }
                }))
                .then(); // 返回 Mono<Void>
    }

    public Mono<Process> getProcessLog(Process process) {
        //使用 Mono.create 以便非阻塞读取进程输出。
        return Mono.create(sink -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.info(line);
                }
                sink.success(process); // 完成后返回 process 对象
            } catch (IOException e) {
                sink.error(e);
            }
        });
    }

    private Mono<Process> getProcess(String videoPath, String outputPath) throws IOException {
        File videoFile = new File(videoPath);
        if (!videoFile.exists()) {
            throw new FileNotFoundException("Input video file does not exist: " + videoPath);
        }

        // 确保路径中的目录存在
        new File(YT1baseDir).mkdirs();

        // 创建命令
        String command = String.format("ffmpeg -i \"%s\" -q:a 0 -map a \"%s\"", videoPath, outputPath);

        // 使用 ProcessBuilder 执行命令
        ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command);
        builder.redirectErrorStream(true); // 将错误流合并到标准输出流
        return Mono.just(builder.start());
    }

    public Mono<Map<String, Object>> downloadFileYT1(String filename) throws IOException {
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

        return Mono.just(map);
    }
}
