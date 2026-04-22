package com.kama.notes.service.impl;

import com.kama.notes.mapper.MessageMapper;
import com.kama.notes.mapper.NoteMapper;
import com.kama.notes.mapper.UserMapper;
import com.kama.notes.model.base.ApiResponse;
import com.kama.notes.model.base.EmptyVO;
import com.kama.notes.model.dto.message.MessageDTO;
import com.kama.notes.model.dto.user.UserQueryParam;
import com.kama.notes.model.entity.Message;
import com.kama.notes.model.entity.Note;
import com.kama.notes.model.entity.Question;
import com.kama.notes.model.entity.User;
import com.kama.notes.model.enums.message.MessageTargetType;
import com.kama.notes.model.enums.message.MessageType;
import com.kama.notes.model.enums.user.UserRole;
import com.kama.notes.model.vo.message.MessageVO;
import com.kama.notes.scope.RequestScopeData;
import com.kama.notes.service.MessageService;
import com.kama.notes.service.QuestionService;
import com.kama.notes.service.UserService;
import com.kama.notes.utils.MessageBuilder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private NoteMapper noteMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RequestScopeData requestScopeData;

    @Override
    public Integer createMessage(MessageDTO messageDTO) {
        try {
            Message message = new Message();
            BeanUtils.copyProperties(messageDTO, message);

            if (messageDTO.getContent() == null) {
                message.setContent("");
            }

            return messageMapper.insert(message);
        } catch (Exception e) {
            throw new RuntimeException("创建消息通知失败: " + e.getMessage());
        }
    }

    @Override
    public Integer batchCreateMessages(List<MessageDTO> messageDTOList) {
        if (messageDTOList == null || messageDTOList.isEmpty()) {
            return 0;
        }
        try {
            List<Message> messages = messageDTOList.stream().map(dto -> {
                Message message = new Message();
                BeanUtils.copyProperties(dto, message);
                if (dto.getContent() == null) {
                    message.setContent("");
                }
                return message;
            }).toList();
            return messageMapper.batchInsert(messages);
        } catch (Exception e) {
            throw new RuntimeException("批量创建消息通知失败: " + e.getMessage());
        }
    }

    @Override
    public ApiResponse<EmptyVO> publishAnnouncement(String content) {
        Long currentUserId = requestScopeData.getUserId();
        if (currentUserId == null) {
            return ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "请先登录");
        }

        User currentUser = userMapper.findById(currentUserId);
        if (currentUser == null) {
            return ApiResponse.error(HttpStatus.NOT_FOUND.value(), "用户不存在");
        }
        if (!Objects.equals(currentUser.getIsAdmin(), UserRole.IS_ADMIN)) {
            return ApiResponse.error(HttpStatus.FORBIDDEN.value(), "只有管理员才能发布系统公告");
        }

        UserQueryParam queryParam = new UserQueryParam();
        queryParam.setPage(1);
        queryParam.setPageSize(Integer.MAX_VALUE);
        List<User> users = userService.getUserList(queryParam).getData();
        if (users == null || users.isEmpty()) {
            return ApiResponse.success();
        }

        List<MessageDTO> messageDTOList = users.stream()
                .map(user -> MessageBuilder.systemAnnouncement(user.getUserId(), content))
                .toList();
        batchCreateMessages(messageDTOList);
        return ApiResponse.success();
    }

    @Override
    public ApiResponse<List<MessageVO>> getMessages() {
        Long currentUserId = requestScopeData.getUserId();
        List<Message> messages = messageMapper.selectByUserId(currentUserId);

        List<Long> senderIds = messages.stream()
                .map(Message::getSenderId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, User> userMap = senderIds.isEmpty()
                ? Collections.emptyMap()
                : userService.getUserMapByIds(senderIds);

        List<MessageVO> messageVOS = messages.stream().map(message -> {
            MessageVO messageVO = new MessageVO();
            BeanUtils.copyProperties(message, messageVO);

            MessageVO.Sender sender = new MessageVO.Sender();
            sender.setUserId(message.getSenderId());
            User senderUser = userMap.get(message.getSenderId());
            if (senderUser != null) {
                sender.setUsername(senderUser.getUsername());
                sender.setAvatarUrl(senderUser.getAvatarUrl());
            } else if (Objects.equals(message.getType(), MessageType.SYSTEM)
                    || Objects.equals(message.getType(), MessageType.ANNOUNCEMENT)) {
                sender.setUsername("系统通知");
            }
            messageVO.setSender(sender);

            if (message.getTargetId() != null && message.getTargetType() != null) {
                MessageVO.Target target = new MessageVO.Target();
                target.setTargetId(message.getTargetId());
                target.setTargetType(message.getTargetType());

                if (Objects.equals(message.getTargetType(), MessageTargetType.NOTE)) {
                    Note note = noteMapper.findById(message.getTargetId());
                    if (note != null) {
                        Question question = questionService.findById(note.getQuestionId());
                        if (question != null) {
                            MessageVO.QuestionSummary questionSummary = new MessageVO.QuestionSummary();
                            questionSummary.setQuestionId(question.getQuestionId());
                            questionSummary.setTitle(question.getTitle());
                            target.setQuestionSummary(questionSummary);
                        }
                    }
                }
                messageVO.setTarget(target);
            }

            return messageVO;
        }).toList();

        return ApiResponse.success(messageVOS);
    }

    @Override
    public ApiResponse<EmptyVO> markAsRead(Integer messageId) {
        Long currentUserId = requestScopeData.getUserId();
        messageMapper.markAsRead(messageId, currentUserId);
        return ApiResponse.success();
    }

    @Override
    public ApiResponse<EmptyVO> markAsReadBatch(List<Integer> messageIds) {
        Long currentUserId = requestScopeData.getUserId();
        messageMapper.markAsReadBatch(messageIds, currentUserId);
        return ApiResponse.success();
    }

    @Override
    public ApiResponse<EmptyVO> markAllAsRead() {
        Long currentUserId = requestScopeData.getUserId();
        messageMapper.markAllAsRead(currentUserId);
        return ApiResponse.success();
    }

    @Override
    public ApiResponse<EmptyVO> deleteMessage(Integer messageId) {
        Long currentUserId = requestScopeData.getUserId();
        messageMapper.deleteMessage(messageId, currentUserId);
        return ApiResponse.success();
    }

    @Override
    public ApiResponse<Integer> getUnreadCount() {
        Long currentUserId = requestScopeData.getUserId();
        Integer count = messageMapper.countUnread(currentUserId);
        return ApiResponse.success(count);
    }
}
