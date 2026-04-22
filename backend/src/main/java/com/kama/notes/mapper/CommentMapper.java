package com.kama.notes.mapper;

import com.kama.notes.model.dto.comment.CommentQueryParams;
import com.kama.notes.model.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 评论Mapper接口
 */
@Mapper
public interface CommentMapper {
    void insert(Comment comment);

    void update(Comment comment);

    void deleteById(Integer commentId);

    Comment findById(Integer commentId);

    List<Comment> findByNoteId(Integer noteId);

    List<Comment> findTopLevelByNoteId(@Param("noteId") Integer noteId,
                                       @Param("sort") String sort,
                                       @Param("pageSize") Integer pageSize,
                                       @Param("offset") Integer offset);

    int countTopLevelByNoteId(Integer noteId);

    List<Comment> findRepliesByRootCommentId(@Param("rootCommentId") Integer rootCommentId,
                                             @Param("sort") String sort,
                                             @Param("pageSize") Integer pageSize,
                                             @Param("offset") Integer offset);

    int countRepliesByRootCommentId(Integer rootCommentId);

    List<Comment> findByQueryParam(@Param("params") CommentQueryParams params,
                                   @Param("pageSize") Integer pageSize,
                                   @Param("offset") Integer offset);

    int countByQueryParam(@Param("params") CommentQueryParams params);

    void incrementLikeCount(Integer commentId);

    void decrementLikeCount(Integer commentId);

    void incrementReplyCount(Integer commentId);

    void decrementReplyCount(Integer commentId);
}
