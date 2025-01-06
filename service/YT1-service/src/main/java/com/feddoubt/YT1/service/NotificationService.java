package com.feddoubt.YT1.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // 推送消息到指定主題
    public void sendNotification(String topic, String message) {
        messagingTemplate.convertAndSend(topic, message);
    }
}
