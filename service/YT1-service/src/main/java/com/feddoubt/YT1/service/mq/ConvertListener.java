package com.feddoubt.YT1.service.mq;

import com.feddoubt.YT1.service.utils.YouTubeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class ConvertListener {

    @Autowired
    private YouTubeUtils youTubeUtils;

    @RabbitListener(queues = "${rabbitmq.convert-queue}")
    public void handleConvert(Map<String, Object> map) {
        try {
            log.info("收到轉換任務: {}", map);
            String videoPath = (String) map.get("output");
            log.info("開始轉換: {}", videoPath);

            // 轉換邏輯
             youTubeUtils.convertToMp3ORMp4(map);

            log.info("轉換完成");
        } catch (Exception e) {
            log.error("處理轉換任務失敗", e);
        }
    }
}