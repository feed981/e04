package com.feddoubt.yt1.service;

import com.feddoubt.yt1.repo.DownloadLogRepository;
import com.feddoubt.model.entity.DownloadLog;
import lombok.extern.slf4j.Slf4j;
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
