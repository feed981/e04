package com.feddoubt.YT1.service.impl;

import com.feddoubt.model.YT1.dtos.YT1Dto;
import com.feddoubt.model.YT1.vos.YT1Vo;
import com.feddoubt.utils.FileUtils;
import com.feddoubt.utils.YouTubeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

@Slf4j
@Service
public class YouTubeService {

    public YT1Vo convertToMp3(YT1Dto dto) throws Exception {
        String url = dto.getUrl();
        log.info("url:{}",url);

        Map<String, String> map = YouTubeUtils.downloadVideo(dto);
        String result = map.get("result");
        String output = map.get("filename");

        log.info("result:{}", result);
        log.info("output:{}", output);
        YT1Vo yt1Vo = new YT1Vo();
        yt1Vo.setFilename(output);
        return yt1Vo;
    }

    public Path downloadFile(String filename) throws IOException, InterruptedException{
        return FileUtils.downloadFileYT1(filename);
    }
}
