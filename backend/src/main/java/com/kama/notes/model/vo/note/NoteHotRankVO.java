package com.kama.notes.model.vo.note;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 笔记热度排行榜项
 */
@Data
public class NoteHotRankVO {
    private Integer noteId;
    private String displayContent;
    private Integer likeCount;
    private Integer commentCount;
    private Integer collectCount;
    private Double hotScore;
    private LocalDateTime createdAt;
    private SimpleAuthorVO author;
    private SimpleQuestionVO question;

    @Data
    public static class SimpleAuthorVO {
        private Long userId;
        private String username;
        private String avatarUrl;
    }

    @Data
    public static class SimpleQuestionVO {
        private Integer questionId;
        private String title;
    }
}
