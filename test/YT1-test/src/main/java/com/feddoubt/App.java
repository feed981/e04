package com.feddoubt;

import com.feddoubt.utils.YouTubeUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class App
{
    private static final Logger logger = LoggerFactory.getLogger(YouTubeUtils.class);

    public static void main( String[] args ) throws IOException, InterruptedException {

        String url = "https://www.youtube.com/watch?v=fSbUGufOt8o";
        // Step 1: 下載影片
        Map<String, String> map = YouTubeUtils.downloadVideo(url);
        String result = map.get("result");
        String mp3Path = "";

        // Step 2: 將影片轉換為 MP3
        if(!result.equals("error") && !result.equals("exist")){
            mp3Path = YouTubeUtils.convertToMp3(map);
        }else{
            mp3Path = map.get("mp3Path");
        }
        logger.info("result:{}", result);
        logger.info("mp3Path:{}", mp3Path);
    }
}
