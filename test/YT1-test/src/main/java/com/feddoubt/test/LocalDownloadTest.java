package com.feddoubt.test;

import org.json.JSONException;
import org.json.JSONObject;
//import com.feddoubt.YT1.YouTubeUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class LocalDownloadTest
{
    private static final Logger logger = LoggerFactory.getLogger(LocalDownloadTest.class);

    public static void main( String[] args ) throws IOException, InterruptedException {

        /*
        https://www.youtube.com/watch?v=fSbUGufOt8o

        https://www.youtube.com/watch?v=qIoDWTF0qSo
        大無限樂團Do As Infinity / 深邃森林（Fukai Mori）
        title:Do As Infinity _ �`ƨ�ˡ]Fukai Mori�^

        https://www.youtube.com/watch?v=cdeXmXY45EA
        🦷為什麽會長蛀牙？ | 😲😈探索兒歌 | 朱妮托尼兒歌 | 好奇心 | Kids Song in Chinese | 兒歌童謠 | 卡通動畫 | 朱妮托尼童話故事
         title:�����_���E���H _ ������q _ ���g������q _ �n�_�� _ Kids Song in Chinese _ ��q���� _ �d�q�ʵe _ ���g�������ܬG��

         */
        String url = "https://www.youtube.com/watch?v=cdeXmXY45EA";
//        String videoTitle = getVideoTitle(url);
//        String videoTitle = YouTubeUtils.getVideoTitle(url);

        Map<String, Object> map = new HashMap<>();
        map.put("filename","【聽故事過節】#冬至 為什麼要 #吃湯圓 ？餛飩的由來竟然跟大美女 #西施 有關？｜小行星樂樂TV.mp3");
        map.put("output","【聽故事過節】#冬至 為什麼要 #吃湯圓 ？餛飩的由來竟然跟大美女 #西施 有關？｜小行星樂樂TV.mp4.webm");
        convertToMp3ORMp4(map);
    }
    
    private static final String ytdlp = "C:\\Tools\\yt-dlp\\yt-dlp.exe";
    private static final String outputDirectory = "C:\\YT1\\download\\";
    private static final String videoPathtDirectory = outputDirectory + "output\\";


    public static void convertToMp3ORMp4(Map<String, Object> map) throws IOException, InterruptedException {
        String outputPath = outputDirectory + (String) map.get("filename");
        String videoPath = videoPathtDirectory + (String) map.get("output");
//        int totalSeconds = (int) map.get("duration");

        // 拼接输出路径
        logger.info("convertToMp3ORMp4 videoPath:{}", videoPath);
        logger.info("convertToMp3ORMp4 outputPath:{}", outputPath);

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
        Process process = builder.start();

        // 读取输出流并解析进度
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                if (line.contains("time=")) {
                    // 提取时间信息（例如 time=00:00:10.50）
                    String timeInfo = line.substring(line.indexOf("time=") + 5, line.indexOf("bitrate=")).trim();
                    System.out.println("Current Progress: " + timeInfo);
                }
            }
        }

        // 等待进程完成
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Conversion failed with exit code " + exitCode);
        }
    }

    public static String getVideoTitle(String url) throws IOException, InterruptedException {
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


    private static String cleanTitle(String title) {
        return title.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

}
