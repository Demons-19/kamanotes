package com.kama.notes.controller;

import com.kama.notes.annotation.NeedLogin;
import com.kama.notes.model.base.ApiResponse;
import com.kama.notes.model.base.EmptyVO;
import com.kama.notes.model.dto.message.CreateAnnouncementRequest;
import com.kama.notes.model.dto.message.MessageQueryParams;
import com.kama.notes.model.request.message.ReadMessageBatchRequest;
import com.kama.notes.model.vo.message.MessageVO;
import com.kama.notes.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 消息控制器
 */
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    /**
     * 获取消息列表
     */
    @GetMapping
    @NeedLogin
    public ApiResponse<List<MessageVO>> getMessages(@Valid MessageQueryParams params) {
        return messageService.getMessages(params);
    }

    @PostMapping("/announcement")
    @NeedLogin
    public ApiResponse<EmptyVO> publishAnnouncement(@Valid @RequestBody CreateAnnouncementRequest request) {
        return messageService.publishAnnouncement(request.getContent());
    }

    @PatchMapping("/{messageId}/read")
    public ApiResponse<EmptyVO> markAsRead(@PathVariable Integer messageId) {
        return messageService.markAsRead(messageId);
    }

    @PatchMapping("/all/read")
    public ApiResponse<EmptyVO> markAllAsRead() {
        return messageService.markAllAsRead();
    }

    @PatchMapping("/batch/read")
    public ApiResponse<EmptyVO> markAsReadBatch(@RequestBody ReadMessageBatchRequest request) {
        return messageService.markAsReadBatch(request.getMessageIds());
    }

    @DeleteMapping("/{messageId}")
    public ApiResponse<EmptyVO> deleteMessage(@PathVariable Integer messageId) {
        return messageService.deleteMessage(messageId);
    }

    @GetMapping("/unread/count")
    public ApiResponse<Integer> getUnreadCount() {
        return messageService.getUnreadCount();
    }
}
