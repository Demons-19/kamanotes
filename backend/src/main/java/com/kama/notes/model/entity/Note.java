package com.kama.notes.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @ClassName Note
 * @Description 笔记实体类
 * @Author Tong
 * @LastChangeDate 2024-12-16 20:01
 * @Version v1.0
 */
@Data
public class Note {
    /**
     * 笔记ID
     */
    private Integer noteId;

    /**
     * 作者ID
     */
    private Long authorId;

    /**
     * 问题ID
     */
    private Integer questionId;

    /**
     * 笔记内容
     */
    private String content;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 评论数
     */
    private Integer commentCount;

    /**
     * 收藏数
     */
    private Integer collectCount;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 审核状态：0审核中 1通过 2拒绝 3审核失败
     */
    private Integer status;

    /**
     * 审核重试次数
     */
    private Integer reviewRetryCount;

    /**
     * 最近一次审核异常信息
     */
    private String reviewLastError;

    /**
     * 审核完成时间
     */
    private LocalDateTime reviewFinishedAt;
}
