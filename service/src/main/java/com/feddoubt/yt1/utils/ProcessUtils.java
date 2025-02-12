package com.feddoubt.YT1.utils;

import com.feddoubt.YT1.config.ConfigProperties;
import com.feddoubt.model.entity.DownloadLog;
import com.feddoubt.model.pojos.VideoDetails;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
    private RabbitTemplate rabbitTemplate;

    public ProcessUtils(ConfigProperties configProperties, RabbitTemplate rabbitTemplate) {
        this.configProperties = configProperties;
        this.rabbitTemplate = rabbitTemplate;
    }

    private String vagrantId;
    private String yt1BaseDir;

    @PostConstruct
    public void init() {
        this.vagrantId = configProperties.getVagrantId();
        this.yt1BaseDir = configProperties.getYt1BaseDir();
    }

    private static final String vagrantfile = "D:\\VirtualBox VMs\\vagrant-ubuntu";


    //避免這些字符導致文件創建失敗
    private String cleanTitle(String title) {
        return title.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    public List<String> dockerCommand(String dockerCommand){
        List<String> command = new ArrayList<>();
        command.add("vagrant");
        command.add("ssh");
        command.add(vagrantId);
        command.add("-c");
        command.add(dockerCommand);
        return command;
    }
    /**
     * vagrant global-status
     * id       name    provider   state   directory
     * ------------------------------------------------------------------------
     * 0d36b1c  default virtualbox running D:/VirtualBox VMs/vagrant-ubuntu
     */
    public VideoDetails dumpjson(DownloadLog downloadLog) throws IOException, InterruptedException {
        VideoDetails videoDetails = new VideoDetails();
        String url = downloadLog.getUrl();
        videoDetails.setUrl(url);
        videoDetails.setFormat(downloadLog.getFormat());
        List<String> command = dockerCommand(String.format("sudo docker compose run ytdlp --dump-json '%s' | jq '{id, title, ext, duration}'", url));
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
                BigDecimal duration = new BigDecimal(json.getInt("duration"));
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
                videoDetails.setPath(yt1BaseDir);

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


    public void commonProcess(List<String> command ,String path) throws IOException, InterruptedException {
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

        // 发现已经下载过相同的文件，它通常会退出并返回 exitCode = 1，表示操作未成功，因为它认为下载的文件已经存在
        int exitCode = process.waitFor();
        log.info("Command completed with exit code: {}", exitCode);

        if (exitCode != 0) {
            // 检查文件是否实际存在
            File file = new File(path);
            if (file.exists() && file.length() > 0) {
                log.info("Despite exit code 1, file exists and is not empty: {}", path);
                return; // 或返回成功状态
            }

            // 如果文件不存在或为空，才抛出异常
            throw new RuntimeException("Download failed with exit code: " + exitCode);
        }
    }

    public void mergeoutput(String url ,String path) throws IOException, InterruptedException {
        commonProcess(
//            dockerCommand(String.format("sudo docker compose run ytdlp -- yt-dlp -o '/downloads/%(title)s.mp4' '%s'",title ,url))
//            dockerCommand(String.format("sudo docker compose run ytdlp -- yt-dlp -o '/downloads/%s.mp4' --no-mtime '%s'",title ,url))
//            dockerCommand(String.format("sudo docker compose run ytdlp -- yt-dlp -f bestvideo+bestaudio --merge-output-format mp4 -o '/downloads/%s.mp4' '%s'",title ,url))
//            dockerCommand(String.format("sudo docker compose run ytdlp -- yt-dlp -f 'bestvideo[ext=mp4]+bestaudio[ext=m4a]' --merge-output-format mp4 --output '/downloads/%s.mp4' --no-keep-video --restrict-filenames '%s'",title ,url))
                dockerCommand(String.format("sudo docker compose run ytdlp -- yt-dlp --config-location /config/yt-dlp.conf -o '%s'" ,url))
                ,path + ".mp4"
        );
    }

    public void ffmpegmp3(String title ,String path)throws IOException, InterruptedException {
        commonProcess(
            dockerCommand(String.format("sudo docker compose run --rm ffmpeg -i '/downloads/%s.mp4' -q:a 0 -map a '/downloads/%s.mp3'",title ,title))
                ,path + ".mp3"
        );
    }
}
