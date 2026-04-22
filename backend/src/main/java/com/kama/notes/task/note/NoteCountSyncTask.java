package com.kama.notes.task.note;

import com.kama.notes.mapper.NoteMapper;
import com.kama.notes.model.entity.Note;
import com.kama.notes.service.RedisService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 笔记点赞数、收藏数、评论数定时同步任务
 * <p>
 * 将 Redis 中缓存的实时计数批量回写到 MySQL，保证最终一致性
 */
@Log4j2
@Component
public class NoteCountSyncTask {

    @Autowired
    private RedisService redisService;

    @Autowired
    private NoteMapper noteMapper;

    private static final String NOTE_LIKE_COUNT_KEY = "note:like_count:";
    private static final String NOTE_COLLECT_COUNT_KEY = "note:collect_count:";
    private static final String NOTE_COMMENT_COUNT_KEY = "note:comment_count:";

    private static final long KEY_EXPIRE_SECONDS = 7 * 24 * 3600;

    /**
     * 每 5 分钟执行一次同步
     */
    @Scheduled(cron = "0 0/5 * * * ?")
    public void syncNoteCounts() {
        log.info("[定时任务] 开始同步笔记点赞数、收藏数、评论数到数据库");
        int likeTotal = syncLikeCounts();
        int collectTotal = syncCollectCounts();
        int commentTotal = syncCommentCounts();
        log.info("[定时任务] 同步完成，点赞数更新 {} 条，收藏数更新 {} 条，评论数更新 {} 条", likeTotal, collectTotal, commentTotal);
    }

    /**
     * 同步点赞数
     */
    private int syncLikeCounts() {
        Set<String> keys = redisService.keys(NOTE_LIKE_COUNT_KEY + "*");
        if (CollectionUtils.isEmpty(keys)) {
            return 0;
        }

        List<Note> notes = new ArrayList<>();

        for (String key : keys) {
            try {
                Integer noteId = Integer.valueOf(key.substring(NOTE_LIKE_COUNT_KEY.length()));
                Object value = redisService.get(key);
                if (value != null) {
                    Integer likeCount = Integer.valueOf(String.valueOf(value));
                    Note note = new Note();
                    note.setNoteId(noteId);
                    note.setLikeCount(likeCount);
                    notes.add(note);
                }
            } catch (Exception e) {
                log.error("[定时任务] 解析点赞数 key 失败, key={}", key, e);
            }
        }

        if (!notes.isEmpty()) {
            try {
                noteMapper.batchUpdateLikeCount(notes);
                keys.forEach(key -> redisService.expire(key, KEY_EXPIRE_SECONDS));
            } catch (Exception e) {
                log.error("[定时任务] 批量更新点赞数失败", e);
            }
        }

        return notes.size();
    }

    /**
     * 同步收藏数
     */
    private int syncCollectCounts() {
        Set<String> keys = redisService.keys(NOTE_COLLECT_COUNT_KEY + "*");
        if (CollectionUtils.isEmpty(keys)) {
            return 0;
        }

        List<Note> notes = new ArrayList<>();

        for (String key : keys) {
            try {
                Integer noteId = Integer.valueOf(key.substring(NOTE_COLLECT_COUNT_KEY.length()));
                Object value = redisService.get(key);
                if (value != null) {
                    Integer collectCount = Integer.valueOf(String.valueOf(value));
                    Note note = new Note();
                    note.setNoteId(noteId);
                    note.setCollectCount(collectCount);
                    notes.add(note);
                }
            } catch (Exception e) {
                log.error("[定时任务] 解析收藏数 key 失败, key={}", key, e);
            }
        }

        if (!notes.isEmpty()) {
            try {
                noteMapper.batchUpdateCollectCount(notes);
                keys.forEach(key -> redisService.expire(key, KEY_EXPIRE_SECONDS));
            } catch (Exception e) {
                log.error("[定时任务] 批量更新收藏数失败", e);
            }
        }

        return notes.size();
    }

    /**
     * 同步评论数
     */
    private int syncCommentCounts() {
        Set<String> keys = redisService.keys(NOTE_COMMENT_COUNT_KEY + "*");
        if (CollectionUtils.isEmpty(keys)) {
            return 0;
        }

        List<Note> notes = new ArrayList<>();

        for (String key : keys) {
            try {
                Integer noteId = Integer.valueOf(key.substring(NOTE_COMMENT_COUNT_KEY.length()));
                Object value = redisService.get(key);
                if (value != null) {
                    Integer commentCount = Integer.valueOf(String.valueOf(value));
                    Note note = new Note();
                    note.setNoteId(noteId);
                    note.setCommentCount(commentCount);
                    notes.add(note);
                }
            } catch (Exception e) {
                log.error("[定时任务] 解析评论数 key 失败, key={}", key, e);
            }
        }

        if (!notes.isEmpty()) {
            try {
                noteMapper.batchUpdateCommentCount(notes);
                keys.forEach(key -> redisService.expire(key, KEY_EXPIRE_SECONDS));
            } catch (Exception e) {
                log.error("[定时任务] 批量更新评论数失败", e);
            }
        }

        return notes.size();
    }
}
