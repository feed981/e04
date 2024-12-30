package com.feddoubt.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class YouTubeUtils {

    private static final Logger logger = LoggerFactory.getLogger(YouTubeUtils.class);

    private static final String ytdlp = "C:\\Tools\\yt-dlp\\yt-dlp.exe";
    private static final String outputDirectory = "C:\\YT1\\download";


    public static boolean isValidYouTubeUrl(String url) {
        return url != null && url.matches("^https?://(www\\.)?youtube\\.com/watch\\?v=.*$");
    }

    public static String getVideoTitle(String url) throws IOException, InterruptedException {
        String command = String.format(ytdlp+" --get-title %s", url);
        Process process = Runtime.getRuntime().exec(command);

        // 讀取命令輸出的標題
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            process.waitFor();
            return reader.readLine(); // 標題是第一行輸出
        }
    }

    public static Map<String, String> downloadVideo(String url) throws IOException, InterruptedException {
        Map<String, String> map = new HashMap<>();
        map.put("result" ,"ok");
        String title = getVideoTitle(url);

        String outputPath = outputDirectory + "\\" + title + ".mp3";
        File file = new File(outputPath);
        // 检查文件是否存在
        if(file.exists()){
            map.put("result" ,"exist");
            map.put("mp3Path" ,outputPath);
            return map;
        }
        map.put("title" ,title);

        logger.info("title:{}", title);
        if (title == null || title.isEmpty()) {
            throw new IOException("Failed to fetch video title.");
        }

        // 替換不允許的文件名字符（Windows 文件系統的限制）
        String sanitizedTitle = title.replaceAll("[\\\\/:*?\"<>|]", "_");
        String output = outputDirectory + "\\output\\" + sanitizedTitle + ".mp4.webm";

        logger.info("output:{}", output);

        String command = String.format(ytdlp+" -o \"%s\" %s", output, url);
        Process process = Runtime.getRuntime().exec(command);
        process.waitFor();

        map.put("output" ,output);
        return map;
    }

    public static String convertToMp3(Map<String, String> map) throws IOException, InterruptedException {
        String title = map.get("title");
        String videoPath = map.get("output");

        // 获取文件名（不带扩展名）
        String fileName = new File(videoPath).getName().replaceFirst("\\.[^.]+$", "");
        logger.info("videoPath:{}", videoPath);
        logger.info("fileName:{}", fileName);

        // 拼接输出路径
        String outputPath = outputDirectory + "\\" + title + ".mp3";
        logger.info("outputPath:{}", outputPath);

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

        return outputPath;
    }

}
