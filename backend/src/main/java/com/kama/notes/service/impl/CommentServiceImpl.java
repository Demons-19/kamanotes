package com.kama.notes.service.impl;

import com.kama.notes.annotation.NeedLogin;
import com.kama.notes.event.MessageEvent;
import com.kama.notes.model.base.ApiResponse;
import com.kama.notes.model.base.EmptyVO;
import com.kama.notes.mapper.CommentMapper;
import com.kama.notes.mapper.NoteMapper;
import com.kama.notes.mapper.UserMapper;
import com.kama.notes.mapper.CommentLikeMapper;
import com.kama.notes.model.base.Pagination;
import com.kama.notes.model.entity.Comment;
import com.kama.notes.model.entity.CommentLike;
import com.kama.notes.model.entity.Note;
import com.kama.notes.model.entity.User;
import com.kama.notes.model.dto.comment.CommentQueryParams;
import com.kama.notes.model.dto.comment.CreateCommentRequest;
import com.kama.notes.model.dto.comment.UpdateCommentRequest;
import com.kama.notes.model.vo.comment.CommentVO;
import com.kama.notes.model.vo.user.UserActionVO;
import com.kama.notes.scope.RequestScopeData;
import com.kama.notes.service.CommentService;
import com.kama.notes.service.NoteService;
import com.kama.notes.service.RedisService;
import com.kama.notes.utils.ApiResponseUtil;
import com.kama.notes.utils.MessageBuilder;
import com.kama.notes.utils.PaginationUtils;
import com.kama.notes.utils.SensitiveWordFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 评论服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {
    private final CommentMapper commentMapper;
    private final NoteMapper noteMapper;
    private final UserMapper userMapper;
    private final CommentLikeMapper commentLikeMapper;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final NoteService noteService;
    private final RequestScopeData requestScopeData;
    private final RedisService redisService;
    private final SensitiveWordFilter sensitiveWordFilter;

    private static final String NOTE_COMMENT_COUNT_KEY = "note:comment_count:";
    private static final int DEFAULT_PREVIEW_REPLY_LIMIT = 2;

    @Override
    @NeedLogin
    @Transactional
    public ApiResponse<Integer> createComment(CreateCommentRequest request) {
        log.info("开始创建评论: request={}", request);

        try {
            Long userId = requestScopeData.getUserId();

            // 获取笔记信息
            Note note = noteMapper.findById(request.getNoteId());
            if (note == null) {
                log.error("笔记不存在: noteId={}", request.getNoteId());
                return ApiResponse.error(HttpStatus.NOT_FOUND.value(), "笔记不存在");
            }

            User sender = userMapper.findById(userId);
            if (sender == null) {
                return ApiResponse.error(HttpStatus.NOT_FOUND.value(), "用户不存在");
            }

            Comment parentComment = null;
            Comment rootComment = null;
            Long replyToUserId = null;
            if (request.getParentId() != null) {
                parentComment = commentMapper.findById(request.getParentId());
                if (parentComment == null) {
                    return ApiResponse.error(HttpStatus.NOT_FOUND.value(), "父评论不存在");
                }
                if (!Objects.equals(parentComment.getNoteId(), request.getNoteId())) {
                    return ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "父评论与笔记不匹配");
                }

                rootComment = parentComment;
                while (rootComment.getParentId() != null && rootComment.getParentId() != 0) {
                    Comment ancestor = commentMapper.findById(rootComment.getParentId());
                    if (ancestor == null) {
                        break;
                    }
                    rootComment = ancestor;
                }

                replyToUserId = request.getReplyToUserId() != null
                        ? request.getReplyToUserId()
                        : parentComment.getAuthorId();
            }

            Comment comment = new Comment();
            comment.setNoteId(request.getNoteId());
            String filteredContent = sensitiveWordFilter.filter(request.getContent());
            comment.setContent(filteredContent);
            comment.setAuthorId(userId);
            comment.setParentId(request.getParentId());
            comment.setRootCommentId(rootComment != null ? rootComment.getCommentId() : null);
            comment.setReplyToUserId(replyToUserId);
            comment.setLikeCount(0);
            comment.setReplyCount(0);
            comment.setCreatedAt(LocalDateTime.now());
            comment.setUpdatedAt(LocalDateTime.now());

            commentMapper.insert(comment);
            if (rootComment == null) {
                comment.setRootCommentId(comment.getCommentId());
                commentMapper.update(comment);
            }
            log.info("评论创建结果: commentId={}", comment.getCommentId());

            // 增加笔记评论数（Redis 计数，异步落库）
            initCommentCountIfAbsent(request.getNoteId());
            redisService.increment(NOTE_COMMENT_COUNT_KEY + request.getNoteId(), 1);

            // 更新热度
            noteService.updateNoteHotScore(request.getNoteId());

            // 如果是回复评论，增加一级父评论的回复数
            if (rootComment != null) {
                commentMapper.incrementReplyCount(rootComment.getCommentId());
            }

            // 发送评论/回复通知（异步事件解耦）
            if (parentComment != null) {
                if (!Objects.equals(replyToUserId, userId)) {
                    applicationEventPublisher.publishEvent(new MessageEvent(this,
                            MessageBuilder.replyComment(userId, sender.getUsername(), replyToUserId, parentComment.getCommentId(), filteredContent)));
                }
            } else if (!Objects.equals(note.getAuthorId(), userId)) {
                applicationEventPublisher.publishEvent(new MessageEvent(this,
                        MessageBuilder.commentNote(userId, sender.getUsername(), note.getAuthorId(), request.getNoteId(), filteredContent)));
            }

            return ApiResponse.success(comment.getCommentId());
        } catch (Exception e) {
            log.error("创建评论失败", e);
            return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "创建评论失败: " + e.getMessage());
        }
    }

    @Override
    @NeedLogin
    @Transactional
    public ApiResponse<EmptyVO> updateComment(Integer commentId, UpdateCommentRequest request) {
        Long userId = requestScopeData.getUserId();

        // 查询评论
        Comment comment = commentMapper.findById(commentId);
        if (comment == null) {
            return ApiResponse.error(HttpStatus.NOT_FOUND.value(), "评论不存在");
        }

        // 检查权限
        if (!comment.getAuthorId().equals(userId)) {
            return ApiResponse.error(HttpStatus.FORBIDDEN.value(), "无权修改该评论");
        }

        try {
            // 更新评论
            String filteredContent = sensitiveWordFilter.filter(request.getContent());
            comment.setContent(filteredContent);
            comment.setUpdatedAt(LocalDateTime.now());
            commentMapper.update(comment);
            return ApiResponse.success(new EmptyVO());
        } catch (Exception e) {
            log.error("更新评论失败", e);
            return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "更新评论失败");
        }
    }

    @Override
    @NeedLogin
    @Transactional
    public ApiResponse<EmptyVO> deleteComment(Integer commentId) {
        Long userId = requestScopeData.getUserId();

        // 查询评论
        Comment comment = commentMapper.findById(commentId);
        if (comment == null) {
            return ApiResponse.error(HttpStatus.NOT_FOUND.value(), "评论不存在");
        }

        // 检查权限
        if (!comment.getAuthorId().equals(userId)) {
            return ApiResponse.error(HttpStatus.FORBIDDEN.value(), "无权删除该评论");
        }

        try {
            // 删除评论
            commentMapper.deleteById(commentId);
            // 减少笔记评论数（Redis 计数，异步落库）并更新热度
            initCommentCountIfAbsent(comment.getNoteId());
            redisService.decrement(NOTE_COMMENT_COUNT_KEY + comment.getNoteId(), 1);
            noteService.updateNoteHotScore(comment.getNoteId());
            return ApiResponse.success(new EmptyVO());
        } catch (Exception e) {
            log.error("删除评论失败", e);
            return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "删除评论失败");
        }
    }

    /**
     * 如果 Redis 中不存在该笔记的评论数，则从数据库读取并初始化
     */
    private void initCommentCountIfAbsent(Integer noteId) {
        String key = NOTE_COMMENT_COUNT_KEY + noteId;
        if (!redisService.exists(key)) {
            Note note = noteMapper.findById(noteId);
            if (note != null) {
                redisService.setIfAbsent(key, note.getCommentCount());
                redisService.expire(key, 7 * 24 * 3600);
            }
        }
    }

    @Override
    public ApiResponse<List<CommentVO>> getComments(CommentQueryParams params) {
        try {
            int offset = PaginationUtils.calculateOffset(params.getPage(), params.getPageSize());
            String normalizedSort = normalizeTopLevelSort(params.getSort());
            List<Comment> topLevelComments = commentMapper.findTopLevelByNoteId(
                    params.getNoteId(), normalizedSort, params.getPageSize(), offset);
            int total = commentMapper.countTopLevelByNoteId(params.getNoteId());
            if (topLevelComments.isEmpty()) {
                return ApiResponseUtil.success("", Collections.emptyList(), new Pagination(params.getPage(), params.getPageSize(), total));
            }

            List<Comment> previewReplies = new ArrayList<>();
            for (Comment rootComment : topLevelComments) {
                previewReplies.addAll(commentMapper.findRepliesByRootCommentId(rootComment.getCommentId(), "asc", DEFAULT_PREVIEW_REPLY_LIMIT, 0));
            }

            List<Comment> mergedComments = new ArrayList<>(topLevelComments);
            mergedComments.addAll(previewReplies);
            Map<Long, User> authorMap = buildAuthorMap(mergedComments);
            Set<Integer> likedSet = buildLikedSet(mergedComments);

            List<CommentVO> result = topLevelComments.stream()
                    .map(comment -> {
                        CommentVO vo = toVO(comment, authorMap, likedSet);
                        vo.setReplyCount(commentMapper.countRepliesByRootCommentId(comment.getCommentId()));
                        vo.setRootCommentId(comment.getCommentId());
                        List<CommentVO> previewVOs = previewReplies.stream()
                                .filter(reply -> Objects.equals(reply.getRootCommentId(), comment.getCommentId()))
                                .map(reply -> {
                                    CommentVO replyVO = toVO(reply, authorMap, likedSet);
                                    replyVO.setRootCommentId(comment.getCommentId());
                                    return replyVO;
                                })
                                .toList();
                        vo.setReplies(previewVOs);
                        return vo;
                    })
                    .toList();

            return ApiResponseUtil.success("", result, new Pagination(params.getPage(), params.getPageSize(), total));
        } catch (Exception e) {
            log.error("获取评论列表失败", e);
            return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "获取评论列表失败");
        }
    }

    @Override
    public ApiResponse<List<CommentVO>> getReplies(Integer rootCommentId, Integer page, Integer pageSize, String sort) {
        try {
            Comment rootComment = commentMapper.findById(rootCommentId);
            if (rootComment == null) {
                return ApiResponse.error(HttpStatus.NOT_FOUND.value(), "一级评论不存在");
            }
            if (rootComment.getParentId() != null && rootComment.getParentId() != 0) {
                return ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "只能查询一级评论下的回复");
            }

            int offset = PaginationUtils.calculateOffset(page, pageSize);
            List<Comment> replies = commentMapper.findRepliesByRootCommentId(rootCommentId, normalizeReplySort(sort), pageSize, offset);
            int total = commentMapper.countRepliesByRootCommentId(rootCommentId);

            List<Comment> mergedComments = new ArrayList<>();
            mergedComments.add(rootComment);
            mergedComments.addAll(replies);
            Map<Long, User> authorMap = buildAuthorMap(mergedComments);
            Set<Integer> likedSet = buildLikedSet(mergedComments);

            List<CommentVO> result = replies.stream()
                    .map(reply -> {
                        CommentVO vo = toVO(reply, authorMap, likedSet);
                        vo.setRootCommentId(rootCommentId);
                        return vo;
                    })
                    .toList();

            return ApiResponseUtil.success("", result, new Pagination(page, pageSize, total));
        } catch (Exception e) {
            log.error("获取回复列表失败", e);
            return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "获取回复列表失败");
        }
    }

    private String normalizeTopLevelSort(String sort) {
        return "hot".equalsIgnoreCase(sort) ? "hot" : "latest";
    }

    private String normalizeReplySort(String sort) {
        return "desc".equalsIgnoreCase(sort) ? "desc" : "asc";
    }

    private Map<Long, User> buildAuthorMap(List<Comment> comments) {
        if (comments == null || comments.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> userIds = comments.stream()
                .flatMap(comment -> java.util.stream.Stream.of(comment.getAuthorId(), comment.getReplyToUserId()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.findByIdBatch(userIds)
                .stream()
                .collect(Collectors.toMap(User::getUserId, user -> user, (a, b) -> a));
    }

    private Set<Integer> buildLikedSet(List<Comment> comments) {
        Long currentUserId = requestScopeData.getUserId();
        if (currentUserId == null || comments == null || comments.isEmpty()) {
            return Collections.emptySet();
        }
        List<Integer> commentIds = comments.stream()
                .map(Comment::getCommentId)
                .distinct()
                .toList();
        if (commentIds.isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<>(commentLikeMapper.findUserLikedCommentIds(currentUserId, commentIds));
    }

    private CommentVO toVO(Comment comment, Map<Long, User> authorMap, Set<Integer> likedSet) {
        CommentVO vo = new CommentVO();
        vo.setCommentId(comment.getCommentId());
        vo.setRootCommentId(comment.getRootCommentId() != null ? comment.getRootCommentId() : comment.getCommentId());
        vo.setNoteId(comment.getNoteId());
        vo.setContent(comment.getContent());
        vo.setLikeCount(comment.getLikeCount());
        vo.setReplyCount(comment.getReplyCount());
        vo.setCreatedAt(comment.getCreatedAt());
        vo.setUpdatedAt(comment.getUpdatedAt());
        vo.setReplies(Collections.emptyList());

        User author = authorMap.get(comment.getAuthorId());
        if (author != null) {
            CommentVO.SimpleAuthorVO authorVO = new CommentVO.SimpleAuthorVO();
            authorVO.setUserId(author.getUserId());
            authorVO.setUsername(author.getUsername());
            authorVO.setAvatarUrl(author.getAvatarUrl());
            vo.setAuthor(authorVO);
        }

        User replyToUser = authorMap.get(comment.getReplyToUserId());
        if (replyToUser != null) {
            CommentVO.SimpleAuthorVO replyToUserVO = new CommentVO.SimpleAuthorVO();
            replyToUserVO.setUserId(replyToUser.getUserId());
            replyToUserVO.setUsername(replyToUser.getUsername());
            replyToUserVO.setAvatarUrl(replyToUser.getAvatarUrl());
            vo.setReplyToUser(replyToUserVO);
        }

        UserActionVO userActions = new UserActionVO();
        userActions.setIsLiked(likedSet.contains(comment.getCommentId()));
        vo.setUserActions(userActions);
        return vo;
    }

    @Override
    @NeedLogin
    @Transactional
    public ApiResponse<EmptyVO> likeComment(Integer commentId) {
        Long userId = requestScopeData.getUserId();

        System.out.println(userId + " liked " + commentId);

        // 查询评论
        Comment comment = commentMapper.findById(commentId);

        if (comment == null) {
            return ApiResponse.error(HttpStatus.NOT_FOUND.value(), "评论不存在");
        }

        try {
            // 增加评论点赞数
            commentMapper.incrementLikeCount(commentId);
            CommentLike commentLike = new CommentLike();

            commentLike.setCommentId(commentId);
            commentLike.setUserId(userId);

            commentLikeMapper.insert(commentLike);

            User sender = userMapper.findById(userId);
            applicationEventPublisher.publishEvent(new MessageEvent(this,
                    MessageBuilder.likeComment(userId, sender.getUsername(), comment.getAuthorId(), comment.getNoteId(), commentId)));
            return ApiResponse.success(new EmptyVO());
        } catch (Exception e) {
            log.error("点赞评论失败", e);
            return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "点赞评论失败");
        }
    }

    @Override
    @NeedLogin
    @Transactional
    public ApiResponse<EmptyVO> unlikeComment(Integer commentId) {
        Long userId = requestScopeData.getUserId();

        // 查询评论
        Comment comment = commentMapper.findById(commentId);
        if (comment == null) {
            return ApiResponse.error(HttpStatus.NOT_FOUND.value(), "评论不存在");
        }

        try {
            // 减少评论点赞数
            commentMapper.decrementLikeCount(commentId);
            commentLikeMapper.delete(commentId, userId);
            return ApiResponse.success(new EmptyVO());
        } catch (Exception e) {
            log.error("取消点赞评论失败", e);
            return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "取消点赞评论失败");
        }
    }
}