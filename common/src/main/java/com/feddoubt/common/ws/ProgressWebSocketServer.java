package com.feddoubt.common.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@Component
@ServerEndpoint("/progress")
public class ProgressWebSocketServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProgressWebSocketServer.class);
    private static Session session;

    @OnOpen
    public void onOpen(Session session) {
        LOGGER.info("WebSocket connection opened: {}", session.getId());
        ProgressWebSocketServer.session = session;
    }

    @OnClose
    public void onClose() {
        LOGGER.info("WebSocket connection closed.");
        ProgressWebSocketServer.session = null;
    }

    public static void sendProgress(int progress) {
        try {
            if (session != null && session.isOpen()) {
                session.getBasicRemote().sendText(String.valueOf(progress));
            }
        } catch (Exception e) {
            LOGGER.error("Error sending progress: ", e);
        }
    }
}
