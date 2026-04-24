package com.kama.notes.service.impl;

import com.kama.notes.mapper.NoteMapper;
import com.kama.notes.mapper.UserMapper;
import com.kama.notes.model.base.ApiResponse;
import com.kama.notes.model.entity.Note;
import com.kama.notes.model.entity.Question;
import com.kama.notes.model.entity.User;
import com.kama.notes.model.vo.note.NoteVO;
import com.kama.notes.service.CollectionNoteService;
import com.kama.notes.service.NoteLikeService;
import com.kama.notes.service.QuestionService;
import com.kama.notes.service.SearchService;
import com.kama.notes.service.UserService;
import com.kama.notes.scope.RequestScopeData;
import com.kama.notes.utils.ApiResponseUtil;
import com.kama.notes.utils.MarkdownUtil;
import com.kama.notes.utils.SearchUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Log4j2
@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private NoteMapper noteMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private UserService userService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private NoteLikeService noteLikeService;

    @Autowired
    private CollectionNoteService collectionNoteService;

    @Autowired
    private RequestScopeData requestScopeData;

    private static final String NOTE_SEARCH_CACHE_KEY = "search:note:%s:%d:%d";
    private static final String NOTE_FULLTEXT_SEARCH_CACHE_KEY = "search:note:fulltext:%s:%d:%d";
    private static final String USER_SEARCH_CACHE_KEY = "search:user:%s:%d:%d";
    private static final String NOTE_TAG_SEARCH_CACHE_KEY = "search:note:tag:%s:%s:%d:%d";
    private static final String NOTE_LIKE_SEARCH_CACHE_KEY = "search:note:like:%s:%d:%d";
    private static final long CACHE_EXPIRE_TIME = 30; // 分钟

    @Override
    public ApiResponse<List<Note>> searchNotes(String keyword, int page, int pageSize) {
        try {
            String cacheKey = String.format(NOTE_SEARCH_CACHE_KEY, keyword, page, pageSize);

            List<Note> cachedResult = (List<Note>) redisTemplate.opsForValue().get(cacheKey);
            if (cachedResult != null) {
                return ApiResponseUtil.success("搜索成功", cachedResult);
            }

            keyword = SearchUtils.preprocessKeyword(keyword);
            int offset = (page - 1) * pageSize;
            List<Note> notes = noteMapper.searchNotes(keyword, pageSize, offset);

            redisTemplate.opsForValue().set(cacheKey, notes, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);

            return ApiResponseUtil.success("搜索成功", notes);
        } catch (Exception e) {
            log.error("搜索笔记失败", e);
            return ApiResponseUtil.error("搜索失败");
        }
    }

    @Override
    public ApiResponse<List<NoteVO>> searchNotesFullText(String keyword, int page, int pageSize) {
        try {
            String cacheKey = String.format(NOTE_FULLTEXT_SEARCH_CACHE_KEY, keyword, page, pageSize);

            List<Note> cachedNotes = (List<Note>) redisTemplate.opsForValue().get(cacheKey);
            if (cachedNotes != null) {
                return ApiResponseUtil.success("搜索成功", buildNoteVOList(cachedNotes));
            }

            keyword = SearchUtils.preprocessKeyword(keyword);
            int offset = (page - 1) * pageSize;
            List<Note> notes = noteMapper.searchNotesByTitleAndContent(keyword, pageSize, offset);

            redisTemplate.opsForValue().set(cacheKey, notes, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);

            return ApiResponseUtil.success("搜索成功", buildNoteVOList(notes));
        } catch (Exception e) {
            log.error("全文搜索笔记失败", e);
            return ApiResponseUtil.error("搜索失败");
        }
    }

    @Override
    public ApiResponse<List<User>> searchUsers(String keyword, int page, int pageSize) {
        try {
            String cacheKey = String.format(USER_SEARCH_CACHE_KEY, keyword, page, pageSize);

            List<User> cachedResult = (List<User>) redisTemplate.opsForValue().get(cacheKey);
            if (cachedResult != null) {
                return ApiResponseUtil.success("搜索成功", cachedResult);
            }

            int offset = (page - 1) * pageSize;
            List<User> users = userMapper.searchUsers(keyword, pageSize, offset);

            redisTemplate.opsForValue().set(cacheKey, users, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);

            return ApiResponseUtil.success("搜索成功", users);
        } catch (Exception e) {
            log.error("搜索用户失败", e);
            return ApiResponseUtil.error("搜索失败");
        }
    }

    @Override
    public ApiResponse<List<Note>> searchNotesByTag(String keyword, String tag, int page, int pageSize) {
        try {
            String cacheKey = String.format(NOTE_TAG_SEARCH_CACHE_KEY, keyword, tag, page, pageSize);

            List<Note> cachedResult = (List<Note>) redisTemplate.opsForValue().get(cacheKey);
            if (cachedResult != null) {
                return ApiResponseUtil.success("搜索成功", cachedResult);
            }

            keyword = SearchUtils.preprocessKeyword(keyword);
            int offset = (page - 1) * pageSize;
            List<Note> notes = noteMapper.searchNotesByTag(keyword, tag, pageSize, offset);

            redisTemplate.opsForValue().set(cacheKey, notes, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);

            return ApiResponseUtil.success("搜索成功", notes);
        } catch (Exception e) {
            log.error("搜索笔记失败", e);
            return ApiResponseUtil.error("搜索失败");
        }
    }

    @Override
    public ApiResponse<List<NoteVO>> searchNotesByLike(String keyword, int page, int pageSize) {
        try {
            String cacheKey = String.format(NOTE_LIKE_SEARCH_CACHE_KEY, keyword, page, pageSize);

            List<Note> cachedNotes = (List<Note>) redisTemplate.opsForValue().get(cacheKey);
            if (cachedNotes != null) {
                return ApiResponseUtil.success("搜索成功", buildNoteVOList(cachedNotes));
            }

            // 模糊匹配不需要分词预处理，保留原始关键词
            int offset = (page - 1) * pageSize;
            List<Note> notes = noteMapper.searchNotesByTitleAndContentLike(keyword, pageSize, offset);

            redisTemplate.opsForValue().set(cacheKey, notes, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);

            return ApiResponseUtil.success("搜索成功", buildNoteVOList(notes));
        } catch (Exception e) {
            log.error("模糊搜索笔记失败", e);
            return ApiResponseUtil.error("搜索失败");
        }
    }

    private List<NoteVO> buildNoteVOList(List<Note> notes) {
        if (notes == null || notes.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> authorIds = notes.stream().map(Note::getAuthorId).distinct().toList();
        List<Integer> questionIds = notes.stream().map(Note::getQuestionId).distinct().toList();
        List<Integer> noteIds = notes.stream().map(Note::getNoteId).toList();

        Map<Long, User> users = userService.getUserMapByIds(authorIds);
        Map<Integer, Question> questions = questionService.getQuestionMapByIds(questionIds);

        Set<Integer> liked = Collections.emptySet();
        Set<Integer> collected = Collections.emptySet();
        if (requestScopeData.isLogin() && requestScopeData.getUserId() != null) {
            Long userId = requestScopeData.getUserId();
            liked = noteLikeService.findUserLikedNoteIds(userId, noteIds);
            collected = collectionNoteService.findUserCollectedNoteIds(userId, noteIds);
        }

        final Set<Integer> finalLiked = liked;
        final Set<Integer> finalCollected = collected;

        return notes.stream()
                .map(note -> toNoteVO(note, users, questions, finalLiked, finalCollected))
                .toList();
    }

    private NoteVO toNoteVO(Note note,
                            Map<Long, User> users,
                            Map<Integer, Question> questions,
                            Set<Integer> liked,
                            Set<Integer> collected) {
        NoteVO vo = new NoteVO();
        BeanUtils.copyProperties(note, vo);

        User user = users.get(note.getAuthorId());
        if (user != null) {
            NoteVO.SimpleAuthorVO author = new NoteVO.SimpleAuthorVO();
            BeanUtils.copyProperties(user, author);
            vo.setAuthor(author);
        }

        Question question = questions.get(note.getQuestionId());
        if (question != null) {
            NoteVO.SimpleQuestionVO questionVO = new NoteVO.SimpleQuestionVO();
            BeanUtils.copyProperties(question, questionVO);
            vo.setQuestion(questionVO);
        }

        NoteVO.UserActionsVO userActions = new NoteVO.UserActionsVO();
        userActions.setIsLiked(liked.contains(note.getNoteId()));
        userActions.setIsCollected(collected.contains(note.getNoteId()));
        vo.setUserActions(userActions);

        // 优化：避免 Markdown 重复解析
        String content = note.getContent();
        if (content != null && !content.isEmpty()) {
            boolean needCollapsed = MarkdownUtil.needCollapsed(content);
            vo.setNeedCollapsed(needCollapsed);
            if (needCollapsed) {
                vo.setDisplayContent(MarkdownUtil.extractIntroduction(content));
            }
        }

        return vo;
    }
}
