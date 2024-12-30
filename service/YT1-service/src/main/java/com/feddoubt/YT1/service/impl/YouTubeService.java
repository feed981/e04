package com.feddoubt.YT1.service.impl;

import com.feddoubt.utils.YouTubeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class YouTubeService {

    public String convertToMp3(String url) throws Exception {
        log.info("url:{}",url);

        Map<String, String> map = YouTubeUtils.downloadVideo(url);
        String result = map.get("result");
        String mp3Path = "";

        // Step 2: 將影片轉換為 MP3
        if(!result.equals("error") && !result.equals("exist")){
            mp3Path = YouTubeUtils.convertToMp3(map);
        }else{
            mp3Path = map.get("mp3Path");
        }

        log.info("result:{}", result);
        log.info("mp3Path:{}", mp3Path);

        return mp3Path;
    }
}
