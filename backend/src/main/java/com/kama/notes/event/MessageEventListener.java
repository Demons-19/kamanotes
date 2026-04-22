package com.kama.notes.event;

import com.kama.notes.mapper.MessageMapper;
import com.kama.notes.mapper.UserMapper;
import com.kama.notes.model.dto.message.MessageDTO;
import com.kama.notes.model.entity.Message;
import com.kama.notes.model.entity.User;
import com.kama.notes.model.enums.message.MessageType;
import com.kama.notes.service.MessageService;
import com.kama.notes.websocket.MessageWebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 消息事件监听器
 * 异步消费消息事件，支持消息聚合与 WebSocket 实时推送
 */
@Component
@Slf4j
public class MessageEventListener {

    @Autowired
    private MessageService messageService;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MessageWebSocketServer webSocketServer;

    @Async
    @EventListener
    public void handleMessageEvent(MessageEvent event) {
        MessageDTO dto = event.getMessageDTO();

        try {
            // 点赞/收藏类型支持聚合，评论不聚合（每条内容不同）
            boolean shouldAggregate = MessageType.LIKE.equals(dto.getType()) || MessageType.COLLECT.equals(dto.getType());

            if (shouldAggregate) {
                Message exist = messageMapper.findRecentAggregateMessage(
                        dto.getReceiverId(), dto.getType(), dto.getTargetId(), dto.getTargetType(), 1
                );
                if (exist != null) {
                    String newContent = buildAggregateContent(exist.getContent(), dto.getSenderName());
                    messageMapper.updateAggregateMessage(exist.getMessageId(), newContent, dto.getSenderId());
                    pushUnreadCount(dto.getReceiverId());
                    return;
                }
            }

            messageService.createMessage(dto);
            pushUnreadCount(dto.getReceiverId());
        } catch (Exception e) {
            log.error("处理消息事件失败", e);
        }
    }

    private String buildAggregateContent(String oldContent, String senderName) {
        if (oldContent.contains("等") && oldContent.contains("人")) {
            try {
                int start = oldContent.indexOf("等") + 1;
                int end = oldContent.indexOf("人");
                int count = Integer.parseInt(oldContent.substring(start, end).trim());
                return senderName + " 等 " + (count + 1) + " 人" + oldContent.substring(end + 1);
            } catch (Exception e) {
                log.warn("解析聚合消息失败: {}", oldContent);
            }
        }
        // 默认聚合为 2 人
        int actionIdx = oldContent.indexOf(" ");
        String action = actionIdx > 0 ? oldContent.substring(actionIdx) : "赞了你的笔记";
        return senderName + " 等 2 人" + action;
    }

    private void pushUnreadCount(Long receiverId) {
        try {
            Integer unreadCount = messageMapper.countUnread(receiverId);
            webSocketServer.sendMessage(receiverId, String.valueOf(unreadCount));
        } catch (Exception e) {
            log.error("WebSocket 推送未读数失败: receiverId={}", receiverId, e);
        }
    }
}
