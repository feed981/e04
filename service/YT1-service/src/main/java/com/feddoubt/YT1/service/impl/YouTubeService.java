package com.feddoubt.YT1.service.impl;

import com.feddoubt.model.YT1.dtos.YT1Dto;
import com.feddoubt.model.YT1.vos.YT1Vo;
import com.feddoubt.utils.YouTubeUtils;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
public class YouTubeService {

    @Autowired
    private YouTubeUtils youTubeUtils;

    public YT1Vo convertToMp3ORMp4(YT1Dto dto) throws Exception {
        String url = dto.getUrl();
        log.info("url:{}",url);

        Map<String, Object> map = youTubeUtils.downloadVideo(dto);
        String result = (String) map.get("result");
        String filename = (String) map.get("filename");

        log.info("result:{}", result);
        log.info("filename:{}", filename);
        YT1Vo yt1Vo = new YT1Vo();
        yt1Vo.setFilename(filename);
        return yt1Vo;
    }

    public Map<String, Object> downloadFile(String filename) throws IOException{
        return youTubeUtils.downloadFileYT1(filename);
    }
}
