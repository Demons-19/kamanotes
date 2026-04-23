package com.kama.notes.service;

import com.kama.notes.model.base.ApiResponse;
import com.kama.notes.model.base.EmptyVO;
import com.kama.notes.model.dto.message.MessageDTO;
import com.kama.notes.model.dto.message.MessageQueryParams;
import com.kama.notes.model.vo.message.MessageVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 消息服务接口
 */
@Transactional
public interface MessageService {
    Integer createMessage(MessageDTO messageDTO);

    Integer batchCreateMessages(List<MessageDTO> messageDTOList);

    ApiResponse<EmptyVO> publishAnnouncement(String content);

    ApiResponse<List<MessageVO>> getMessages(MessageQueryParams params);

    ApiResponse<EmptyVO> markAsRead(Integer messageId);

    ApiResponse<EmptyVO> markAsReadBatch(List<Integer> messageIds);

    ApiResponse<EmptyVO> markAllAsRead();

    ApiResponse<EmptyVO> deleteMessage(Integer messageId);

    ApiResponse<Integer> getUnreadCount();
}
