package com.feddoubt.YT1.service.impl;

import com.feddoubt.utils.YouTubeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class YouTubeService {

    public String convertToMp3(String url) throws Exception {
        log.info("url:{}",url);
        // Step 1: 下載影片
        String videoPath = YouTubeUtils.downloadVideo(url);

        // Step 2: 將影片轉換為 MP3
        String mp3Path = YouTubeUtils.convertToMp3(videoPath);

        return mp3Path;
    }
}
