package com.feddoubt.YT1.service;

import com.feddoubt.YT1.repo.DownloadLogRepository;
import com.feddoubt.YT1.repo.UserLogRepository;
import com.feddoubt.common.YT1.config.message.RabbitResponse;
import com.feddoubt.model.YT1.entity.DownloadLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class DownloadLogService {

    private final DownloadLogRepository downloadLogRepository;

    public DownloadLogService(DownloadLogRepository downloadLogRepository) {
        this.downloadLogRepository = downloadLogRepository;
    }

    public void saveDownloadLog(DownloadLog downloadLog){
        downloadLogRepository.save(downloadLog);
    }

    public String findByTitleAndExt(String title ,String ext){
        return downloadLogRepository.findByTitleAndExt(title ,ext).orElse(null);
    }
}
