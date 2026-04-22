package com.kama.notes.utils;

import com.kama.notes.model.dto.message.MessageDTO;
import com.kama.notes.model.enums.message.MessageTargetType;
import com.kama.notes.model.enums.message.MessageType;

/**
 * 消息构建器，统一封装各类通知的 DTO 生成逻辑
 */
public class MessageBuilder {

    private static final Long SYSTEM_SENDER_ID = 0L;
    private static final String SYSTEM_SENDER_NAME = "系统通知";

    public static MessageDTO likeNote(Long senderId, String senderName, Long receiverId, Integer noteId) {
        MessageDTO dto = new MessageDTO();
        dto.setType(MessageType.LIKE);
        dto.setTargetType(MessageTargetType.NOTE);
        dto.setTargetId(noteId);
        dto.setReceiverId(receiverId);
        dto.setSenderId(senderId);
        dto.setSenderName(senderName);
        dto.setIsRead(false);
        dto.setContent(senderName + " 赞了你的笔记");
        return dto;
    }

    public static MessageDTO likeComment(Long senderId, String senderName, Long receiverId, Integer noteId, Integer commentId) {
        MessageDTO dto = new MessageDTO();
        dto.setType(MessageType.LIKE);
        dto.setTargetType(MessageTargetType.COMMENT);
        dto.setTargetId(commentId);
        dto.setReceiverId(receiverId);
        dto.setSenderId(senderId);
        dto.setSenderName(senderName);
        dto.setIsRead(false);
        dto.setContent(senderName + " 赞了你的评论");
        return dto;
    }

    public static MessageDTO commentNote(Long senderId, String senderName, Long receiverId, Integer noteId, String content) {
        MessageDTO dto = new MessageDTO();
        dto.setType(MessageType.COMMENT);
        dto.setTargetType(MessageTargetType.NOTE);
        dto.setTargetId(noteId);
        dto.setReceiverId(receiverId);
        dto.setSenderId(senderId);
        dto.setSenderName(senderName);
        dto.setIsRead(false);
        dto.setContent(senderName + " 评论了你的笔记: " + content);
        return dto;
    }

    public static MessageDTO replyComment(Long senderId, String senderName, Long receiverId, Integer commentId, String content) {
        MessageDTO dto = new MessageDTO();
        dto.setType(MessageType.REPLY);
        dto.setTargetType(MessageTargetType.COMMENT);
        dto.setTargetId(commentId);
        dto.setReceiverId(receiverId);
        dto.setSenderId(senderId);
        dto.setSenderName(senderName);
        dto.setIsRead(false);
        dto.setContent(senderName + " 回复了你的评论: " + content);
        return dto;
    }

    public static MessageDTO collectNote(Long senderId, String senderName, Long receiverId, Integer noteId) {
        MessageDTO dto = new MessageDTO();
        dto.setType(MessageType.COLLECT);
        dto.setTargetType(MessageTargetType.NOTE);
        dto.setTargetId(noteId);
        dto.setReceiverId(receiverId);
        dto.setSenderId(senderId);
        dto.setSenderName(senderName);
        dto.setIsRead(false);
        dto.setContent(senderName + " 收藏了你的笔记");
        return dto;
    }

    public static MessageDTO noteAuditRejected(Long receiverId, Integer noteId) {
        MessageDTO dto = new MessageDTO();
        dto.setType(MessageType.SYSTEM);
        dto.setTargetType(MessageTargetType.NOTE);
        dto.setTargetId(noteId);
        dto.setReceiverId(receiverId);
        dto.setSenderId(SYSTEM_SENDER_ID);
        dto.setSenderName(SYSTEM_SENDER_NAME);
        dto.setIsRead(false);
        dto.setContent("你的笔记因内容审核未通过，当前未发布，请修改内容后重新提交");
        return dto;
    }

    public static MessageDTO noteAuditApproved(Long receiverId, Integer noteId) {
        MessageDTO dto = new MessageDTO();
        dto.setType(MessageType.SYSTEM);
        dto.setTargetType(MessageTargetType.NOTE);
        dto.setTargetId(noteId);
        dto.setReceiverId(receiverId);
        dto.setSenderId(SYSTEM_SENDER_ID);
        dto.setSenderName(SYSTEM_SENDER_NAME);
        dto.setIsRead(false);
        dto.setContent("你的笔记已审核通过，现已成功发布");
        return dto;
    }

    public static MessageDTO systemAnnouncement(Long receiverId, String content) {
        MessageDTO dto = new MessageDTO();
        dto.setType(MessageType.ANNOUNCEMENT);
        dto.setTargetType(MessageTargetType.NONE);
        dto.setTargetId(0);
        dto.setReceiverId(receiverId);
        dto.setSenderId(SYSTEM_SENDER_ID);
        dto.setSenderName(SYSTEM_SENDER_NAME);
        dto.setIsRead(false);
        dto.setContent(content);
        return dto;
    }
}
