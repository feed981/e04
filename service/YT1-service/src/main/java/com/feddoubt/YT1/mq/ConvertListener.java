package com.feddoubt.YT1.mq;

import com.feddoubt.YT1.utils.ProcessUtils;
import com.feddoubt.model.YT1.entity.DownloadLog;
import com.feddoubt.model.YT1.pojos.VideoDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;


import java.io.File;

@Slf4j
@Service
public class ConvertListener {

    private final ProcessUtils processUtils;
    private RabbitTemplate rabbitTemplate;

    public ConvertListener(ProcessUtils processUtils , RabbitTemplate rabbitTemplate){
        this.rabbitTemplate = rabbitTemplate;
        this.processUtils = processUtils;
    }


    @RabbitListener(queues = "${rabbitmq.convert-queue}")
    public void handleConvert(DownloadLog downloadLog) {
        try {
            log.info("異步下載任務前檢查...");
            VideoDetails videoDetails = processUtils.dumpjson(downloadLog);

            log.info("下載任務前檔案是否存在...");
            String url = videoDetails.getUrl();
            String title = videoDetails.getTitle();
            String path = videoDetails.getPath();
            log.info("base dir path:{}",path);

            if(!new File(path + ".mp4").exists()){
                log.info("開始執行mp4下載任務...");
                processUtils.mergeoutput(url);
            }

            String format = videoDetails.getFormat();
            if(format.equals("mp3")){
                if(!new File(path + ".mp3").exists()) {
                    log.info("開始執行mp3轉換任務...");
                    processUtils.ffmpegmp3(title);
                }
            }

            log.info("開始通知任務.  ..");
            String filename = title + format;
            rabbitTemplate.convertAndSend("notificationQueue", filename);

        } catch (Exception e) {
            log.error("處理下載任務失敗", e);
        }
    }
}