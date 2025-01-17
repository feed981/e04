package com.feddoubt.YT1.utils;

import com.feddoubt.YT1.config.ConfigProperties;
import com.feddoubt.common.YT1.config.message.ApiResponse;
import com.feddoubt.common.YT1.config.message.CustomHttpStatus;
import com.feddoubt.common.YT1.config.message.RabbitResponse;
import com.feddoubt.common.YT1.config.message.ResponseUtils;
import com.feddoubt.model.YT1.entity.DownloadLog;
import com.feddoubt.model.YT1.pojos.VideoDetails;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class ProcessUtils {

    private final ConfigProperties configProperties;
    private final RabbitResponse rabbitResponse;
    private RabbitTemplate rabbitTemplate;

    public ProcessUtils(ConfigProperties configProperties, RabbitTemplate rabbitTemplate, RabbitResponse rabbitResponse) {
        this.configProperties = configProperties;
        this.rabbitResponse = rabbitResponse;
        this.rabbitTemplate = rabbitTemplate;
    }

    private String vagrantId;
    private String yt1baseDir;
    private String ytdlpContainerName;
    private String ffmpegContainerName;

    @PostConstruct
    public void init() {
        this.vagrantId = configProperties.getVagrantId();
        this.yt1baseDir = configProperties.getYt1BaseDir();
        this.ytdlpContainerName = configProperties.getYtdlpContainName();
        this.ffmpegContainerName = configProperties.getFfmpegContainName();
    }

    private static final String vagrantfile = "D:\\VirtualBox VMs\\vagrant-ubuntu";

    /**
     * vagrant global-status
     * id       name    provider   state   directory
     * ------------------------------------------------------------------------
     * 0d36b1c  default virtualbox running D:/VirtualBox VMs/vagrant-ubuntu
     */
    public VideoDetails beforeProcessVideoDownload(DownloadLog downloadLog) throws IOException, InterruptedException {
        VideoDetails videoDetails = new VideoDetails();
        String url = downloadLog.getUrl();
        videoDetails.setUrl(url);
        videoDetails.setFormat(downloadLog.getFormat());
        //sudo docker compose run ytdlp --dump-json "https://www.youtube.com/watch?v=RhSJ3AGQLkM" | jq '{id, title, ext, duration}'
        List<String> command = new ArrayList<>();
        command.add("vagrant");
        command.add("ssh");
        command.add(vagrantId);
        command.add("-c");
        String dockerCommand = String.format("sudo docker compose run ytdlp --dump-json '%s' | jq '{id, title, ext, duration}'", url);
        command.add(dockerCommand);
        log.info("Executing command: {}", String.join(" ", command));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true); // 合併標準錯誤流到標準輸出流
        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            boolean insideJson = false;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.trim().startsWith("{")) {
                    insideJson = true;
                }

                if (insideJson) {
                    stringBuilder.append(line).append("\n");
                }

                if (insideJson && line.trim().endsWith("}")) {
                    break; // 找到完整的 JSON 就結束
                }
            }
            process.waitFor();
            log.info("stringBuilder:{}",stringBuilder.toString());

            if (stringBuilder.length() == 0) {
                videoDetails.setErrorMessage("length = 0，無法獲取視頻標題。請檢查 URL 或 yt-dlp 命令");
                return null;
//                throw new IOException("無法獲取視頻標題。請檢查 URL 或 yt-dlp 命令。");
            }

            if(stringBuilder.toString().contains("ERROR")){
                log.error(stringBuilder.toString());
                videoDetails.setErrorMessage(stringBuilder.toString());
                return null;
            }

            // 解析 JSON 輸出
            try {
                JSONObject json = new JSONObject(stringBuilder.toString());
                BigDecimal duration = new BigDecimal(json.getString("duration"));
                // 超過10分鐘
                if(duration.compareTo(new BigDecimal(600)) > 0){
                    videoDetails.setErrorMessage("video length too long");
                    return null;
                }

                videoDetails.setVideoId(json.getString("id"));
                String title = cleanTitle(json.getString("title"));
                String sanitizedTitle = title.replaceAll("[\\\\/:*?\"<>|.]", "_").replaceAll("\\.\\.", "_");
                String ext = json.getString("ext");
                videoDetails.setTitle(sanitizedTitle);
                videoDetails.setExt(ext);
                videoDetails.setDuration(duration);
                videoDetails.setConvertFilename();
                videoDetails.setDownloadFilename();
                videoDetails.setConvertVideoPath(yt1baseDir + videoDetails.getConvertFilename());
                videoDetails.setDownloadVideoPath(yt1baseDir + sanitizedTitle + ext);

                downloadLog.setTitle(title);
                downloadLog.setExt(ext);
                rabbitTemplate.convertAndSend("downloadLogQueue", downloadLog);

            } catch (JSONException e) {
                log.error(e.getMessage());
                throw new RuntimeException(e);
            }

            log.info("videoDetails:{}",videoDetails);
            return videoDetails;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    //避免這些字符導致文件創建失敗
    private String cleanTitle(String title) {
        return title.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    /**
     *     mp4: sudo docker compose run ytdlp -- yt-dlp -f "bestaudio" --extract-audio --audio-format mp3 -o "/downloads/%(title)s.%(ext)s" "https://www.youtube.com/watch?v=mnsqi3lQNbY"
     *     mp4: sudo docker compose run ytdlp -- yt-dlp -f bestvideo+bestaudio --merge-output-format mp4 -o "/downloads/%(title)s.%(ext)s" "https://www.youtube.com/watch?v=mnsqi3lQNbY"
     *
     *     mp3: sudo docker compose run --rm ffmpeg -i "/downloads/Gran Turismo 5 OST： Yuki Oike - Passion [mnsqi3lQNbY].mp4" -q:a 0 -map a "/downloads/output.mp3"
     */
    public void processVideoDownload(String url) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add("vagrant");
        command.add("ssh");
        command.add(vagrantId);
        command.add("-c");
        String dockerCommand = String.format("sudo docker compose run ytdlp -- yt-dlp -f bestvideo+bestaudio --merge-output-format mp4 -o '/downloads/%(title)s' '%s'",url);
        command.add(dockerCommand);
        log.info("Executing command: {}", String.join(" ", command));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        // 非阻塞讀取輸出
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("Output: {}", line);
            }
        }

        int exitCode = process.waitFor();
        log.info("Command completed with exit code: {}", exitCode);

        if (exitCode != 0) {
            throw new RuntimeException("Download failed with exit code: " + exitCode);
        }
        log.info("downloadVideo end................");
    }

    // ytdlp , ffmepg
    public <T> ApiResponse<String> commonProcess(String dockerCommand , String logstr, String queue , T t) {
        log.info("dockerCommand:{}",dockerCommand);

        try {
            Process process = Runtime.getRuntime().exec(dockerCommand);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info(line);
                }
            }

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

}
