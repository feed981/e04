package com.feddoubt.YT1.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProgressNotifier {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void notifyProgress(String taskId, int progress) {
        messagingTemplate.convertAndSend("/progress/" + taskId, progress);
    }
}
