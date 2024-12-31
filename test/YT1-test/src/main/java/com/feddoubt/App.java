package com.feddoubt;

import com.feddoubt.model.YT1.dtos.YT1Dto;
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

        YT1Dto yt1Dto = new YT1Dto();
        yt1Dto.setUrl(url);
        yt1Dto.setFormat("mp3");
        Map<String, String> map = YouTubeUtils.downloadVideo(yt1Dto);
        String result = map.get("result");
        String mp3Path = "";

        logger.info("result:{}", result);
        logger.info("mp3Path:{}", mp3Path);
    }
}
