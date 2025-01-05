package com.feddoubt.YT1.service.mq;

import com.feddoubt.YT1.service.utils.YouTubeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
public class DownloadListener {

    private final RabbitTemplate rabbitTemplate;

    public DownloadListener(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Autowired
    private YouTubeUtils youTubeUtils;

    @RabbitListener(queues = "${rabbitmq.download-queue}")
    @Async
    public Mono<Void> handleDownload(Map<String, Object> map) {
        log.info("開始執行異步下載任務...");

        String command = (String) map.get("command");
        log.info("處理下載命令: {}", command);

        return Mono.fromCallable(() -> Runtime.getRuntime().exec(command))
                .flatMap(process -> youTubeUtils.getProcessLog(process)
                        .thenReturn(process) // 确保 `Process` 对象继续传递
                )
                .flatMap(process -> Mono.fromCallable(() -> {
                    try {
                        int exitCode = process.waitFor();
                        if (exitCode != 0) {
                            throw new IOException("下載失敗，退出碼: " + exitCode);
                        }
                        log.info("命令執行完成，退出碼: {}", exitCode);
                        return null;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Process was interrupted", e);
                    }
                }))
                //是副作用操作，在流中确保消息发送不会中断主逻辑
                .doOnSuccess(exitCode -> {
                    log.info("下載完成，發送到轉換隊列...");
                    rabbitTemplate.convertAndSend("convertQueue", map);
                })
                //异步流中的异常会通过 onErrorResume 捕获，不需要用 try-catch 包裹整个逻辑
                .onErrorResume(e -> {
                    log.error("處理下載命令失敗", e);
                    return Mono.empty(); // 捕获错误，避免任务失败
                }).then();
    }
}