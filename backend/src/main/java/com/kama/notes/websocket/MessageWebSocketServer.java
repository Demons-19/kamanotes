package com.kama.notes.websocket;

import lombok.extern.slf4j.Slf4j;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息 WebSocket 服务端
 * 按 userId 维护长连接，用于实时推送未读消息数
 */
@ServerEndpoint("/ws/message/{userId}")
@Slf4j
public class MessageWebSocketServer {

    private static final ConcurrentHashMap<Long, Session> SESSION_MAP = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Long userId) {
        SESSION_MAP.put(userId, session);
        log.info("WebSocket 连接建立: userId={}", userId);
    }

    @OnClose
    public void onClose(@PathParam("userId") Long userId) {
        SESSION_MAP.remove(userId);
        log.info("WebSocket 连接关闭: userId={}", userId);
    }

    @OnError
    public void onError(Session session, Throwable error, @PathParam("userId") Long userId) {
        log.error("WebSocket 错误: userId={}", userId, error);
        SESSION_MAP.remove(userId);
    }

    public void sendMessage(Long userId, String message) {
        Session session = SESSION_MAP.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                log.error("WebSocket 发送消息失败: userId={}", userId, e);
            }
        }
    }
}
