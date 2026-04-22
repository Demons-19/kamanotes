package com.kama.notes.event;

import com.kama.notes.model.dto.message.MessageDTO;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 消息事件
 * 用于系统内部的消息传递，解耦业务与消息写入
 */
@Getter
public class MessageEvent extends ApplicationEvent {

    private final MessageDTO messageDTO;

    public MessageEvent(Object source, MessageDTO messageDTO) {
        super(source);
        this.messageDTO = messageDTO;
    }
}
