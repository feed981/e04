package com.feddoubt.YT1.service.impl;

import com.feddoubt.YT1.service.IYouTubeService;
import com.feddoubt.YT1.service.utils.YouTubeUtils;
import com.feddoubt.common.YT1.dtos.YT1Dto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
//

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
public class YouTubeServiceImpl implements IYouTubeService {

    @Autowired
    private YouTubeUtils youTubeUtils;

    @Override
    public String convertToMp3ORMp4(YT1Dto dto) throws Exception {
        return youTubeUtils.downloadVideo(dto);
    }

    @Override
    public Map<String, Object> downloadFile(String filename) throws IOException{
        return youTubeUtils.downloadFileYT1(filename);
    }

}
