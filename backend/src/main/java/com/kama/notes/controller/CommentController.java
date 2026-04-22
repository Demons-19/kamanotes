package com.kama.notes.controller;

import com.kama.notes.model.base.ApiResponse;
import com.kama.notes.model.base.EmptyVO;
import com.kama.notes.model.dto.comment.CommentQueryParams;
import com.kama.notes.model.dto.comment.CreateCommentRequest;
import com.kama.notes.model.dto.comment.UpdateCommentRequest;
import com.kama.notes.model.vo.comment.CommentVO;
import com.kama.notes.service.CommentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import java.util.List;

/**
 * 评论控制器
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class CommentController {

    @Autowired
    private CommentService commentService;

    /**
     * 获取一级评论列表
     *
     * @param params 查询参数
     * @return 评论列表
     */
    @GetMapping("/comments")
    public ApiResponse<List<CommentVO>> getComments(
            @Valid CommentQueryParams params) {
        return commentService.getComments(params);
    }

    /**
     * 获取某个一级评论下的回复列表
     */
    @GetMapping("/comments/{rootCommentId}/replies")
    public ApiResponse<List<CommentVO>> getReplies(
            @PathVariable("rootCommentId") Integer rootCommentId,
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) Integer pageSize,
            @RequestParam(defaultValue = "asc") String sort) {
        return commentService.getReplies(rootCommentId, page, pageSize, sort);
    }

    /**
     * 创建评论
     *
     * @param request 创建评论请求
     * @return 创建的评论ID
     */
    @PostMapping("/comments")
    public ApiResponse<Integer> createComment(
            @Valid
            @RequestBody
            CreateCommentRequest request) {
        return commentService.createComment(request);
    }

    /**
     * 更新评论
     *
     * @param commentId 评论ID
     * @param request   更新评论请求
     * @return 空响应
     */
    @PatchMapping("/comments/{commentId}")
    public ApiResponse<EmptyVO> updateComment(
            @PathVariable("commentId") Integer commentId,
            @Valid
            @RequestBody
            UpdateCommentRequest request) {
        return commentService.updateComment(commentId, request);
    }

    /**
     * 删除评论
     *
     * @param commentId 评论ID
     * @return 空响应
     */
    @DeleteMapping("/comments/{commentId}")
    public ApiResponse<EmptyVO> deleteComment(
            @PathVariable("commentId") Integer commentId) {
        return commentService.deleteComment(commentId);
    }

    /**
     * 点赞评论
     *
     * @param commentId 评论ID
     * @return 空响应
     */
    @PostMapping("/comments/{commentId}/like")
    public ApiResponse<EmptyVO> likeComment(
            @PathVariable("commentId") Integer commentId) {
        return commentService.likeComment(commentId);
    }

    /**
     * 取消点赞评论
     *
     * @param commentId 评论ID
     * @return 空响应
     */
    @DeleteMapping("/comments/{commentId}/like")
    public ApiResponse<EmptyVO> unlikeComment(
            @PathVariable("commentId") Integer commentId) {
        return commentService.unlikeComment(commentId);
    }
}
