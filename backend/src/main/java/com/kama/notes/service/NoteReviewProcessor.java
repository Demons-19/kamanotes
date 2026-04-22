package com.kama.notes.service;

import org.springframework.data.redis.connection.stream.MapRecord;

public interface NoteReviewProcessor {
    void process(MapRecord<String, Object, Object> record);
}
