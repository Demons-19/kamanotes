package com.kama.notes.model.dto.message;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class CreateAnnouncementRequest {
    @NotBlank(message = "公告内容不能为空")
    private String content;
}
