package com.kama.notes.service.impl;

import com.kama.notes.config.NoteReviewProperties;
import com.kama.notes.model.enums.redisKey.RedisKey;
import com.kama.notes.service.NoteReviewQueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class NoteReviewQueueServiceImpl implements NoteReviewQueueService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private NoteReviewProperties properties;

    @Override
    public void enqueue(Integer noteId, Long userId) {
        Map<String, String> payload = new HashMap<>();
        payload.put("noteId", String.valueOf(noteId));
        payload.put("userId", String.valueOf(userId));
        stringRedisTemplate.opsForStream().add(MapRecord.create(properties.getStreamKey(), payload));
    }

    @Override
    public void enqueueRetry(Integer noteId, Long userId, long executeAtEpochMilli) {
        String member = noteId + ":" + userId;
        stringRedisTemplate.opsForZSet().add(RedisKey.noteReviewRetryZSet(), member, executeAtEpochMilli);
    }
}
