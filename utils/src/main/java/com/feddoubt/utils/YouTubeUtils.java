package com.feddoubt.utils;

import java.io.IOException;

public class YouTubeUtils {

    public static boolean isValidYouTubeUrl(String url) {
        return url != null && url.matches("^https?://(www\\.)?youtube\\.com/watch\\?v=.*$");
    }

    public static String downloadVideo(String url) throws IOException, InterruptedException {
        String output = "video.mp4";
        String command = String.format("yt-dlp -o %s %s", output, url);
        Process process = Runtime.getRuntime().exec(command);
        process.waitFor();
        return output;
    }

    public static String convertToMp3(String videoPath) throws IOException, InterruptedException {
        String output = videoPath.replace(".mp4", ".mp3");
        String command = String.format("ffmpeg -i %s -q:a 0 -map a %s", videoPath, output);
        Process process = Runtime.getRuntime().exec(command);
        process.waitFor();
        return output;
    }
}
