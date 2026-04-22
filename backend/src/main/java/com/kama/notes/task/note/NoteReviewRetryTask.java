package com.kama.notes.task.note;

import com.kama.notes.config.NoteReviewProperties;
import com.kama.notes.model.entity.Note;
import com.kama.notes.model.enums.redisKey.RedisKey;
import com.kama.notes.mapper.NoteMapper;
import com.kama.notes.service.NoteReviewQueueService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
public class NoteReviewRetryTask {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private NoteReviewProperties properties;

    @Autowired
    private NoteReviewQueueService noteReviewQueueService;

    @Autowired
    private NoteMapper noteMapper;

    @Scheduled(fixedDelay = 1000)
    public void moveRetryTasks() {
        long now = System.currentTimeMillis();
        Set<String> members = stringRedisTemplate.opsForZSet()
                .rangeByScore(RedisKey.noteReviewRetryZSet(), 0, now, 0, properties.getBatchSize());
        if (members == null || members.isEmpty()) {
            return;
        }

        for (String member : members) {
            if (member == null || !member.contains(":")) {
                stringRedisTemplate.opsForZSet().remove(RedisKey.noteReviewRetryZSet(), member);
                continue;
            }
            String[] parts = member.split(":", 2);
            Integer noteId;
            Long userId;
            try {
                noteId = Integer.parseInt(parts[0]);
                userId = Long.parseLong(parts[1]);
            } catch (NumberFormatException e) {
                stringRedisTemplate.opsForZSet().remove(RedisKey.noteReviewRetryZSet(), member);
                continue;
            }

            Note note = noteMapper.findRawById(noteId);
            if (note == null || note.getStatus() == null || note.getStatus() != 0) {
                stringRedisTemplate.opsForZSet().remove(RedisKey.noteReviewRetryZSet(), member);
                continue;
            }

            Long removed = stringRedisTemplate.opsForZSet().remove(RedisKey.noteReviewRetryZSet(), member);
            if (removed != null && removed > 0) {
                noteReviewQueueService.enqueue(noteId, userId);
            }
        }
    }
}
