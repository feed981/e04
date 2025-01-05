package com.feddoubt.YT1.service;

import com.feddoubt.common.YT1.dtos.YT1Dto;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Map;

public interface IYouTubeService {
    public Mono<String> convertToMp3ORMp4(YT1Dto dto) throws Exception;
    public Mono<Map<String, Object>> downloadFile(String filename) throws IOException;
}