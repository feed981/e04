package com.feddoubt.YT1.service;

import com.feddoubt.model.YT1.dtos.YT1Dto;

import java.io.IOException;
import java.util.Map;

public interface IYouTubeService {
    public String convertToMp3ORMp4(YT1Dto dto) throws Exception;
    public Map<String, Object> downloadFile(String filename) throws IOException;
}
