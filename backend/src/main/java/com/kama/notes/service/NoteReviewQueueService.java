package com.kama.notes.service;

public interface NoteReviewQueueService {
    void enqueue(Integer noteId, Long userId);

    void enqueueRetry(Integer noteId, Long userId, long executeAtEpochMilli);
}
