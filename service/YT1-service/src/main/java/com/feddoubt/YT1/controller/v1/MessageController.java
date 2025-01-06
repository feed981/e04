package com.feddoubt.YT1.controller.v1;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
public class MessageController {

    @MessageMapping("/send") // 映射到 /app/send 的消息
    @SendTo("/topic/convert") // 广播到 /topic/messages 的所有订阅者
    public String sendMessage(String message) {
        return message; // 将接收到的消息返回给所有订阅者
    }
}
