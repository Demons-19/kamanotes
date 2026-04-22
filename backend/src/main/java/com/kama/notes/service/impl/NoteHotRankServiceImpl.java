package com.kama.notes.service.impl;

import com.kama.notes.mapper.NoteMapper;
import com.kama.notes.model.entity.Note;
import com.kama.notes.model.enums.redisKey.RedisKey;
import com.kama.notes.service.NoteHotRankService;
import com.kama.notes.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NoteHotRankServiceImpl implements NoteHotRankService {

    @Autowired
    private NoteMapper noteMapper;

    @Autowired
    private RedisService redisService;

    private static final String NOTE_LIKE_COUNT_KEY = "note:like_count:";
    private static final String NOTE_COLLECT_COUNT_KEY = "note:collect_count:";
    private static final String NOTE_COMMENT_COUNT_KEY = "note:comment_count:";

    @Override
    public void updateNoteHotScore(Integer noteId) {
        Note note = noteMapper.findById(noteId);
        if (note == null) {
            return;
        }
        double hotScore = calculateHotScore(note);
        redisService.zAdd(RedisKey.noteHotRank(), String.valueOf(noteId), hotScore);
    }

    @Override
    public void removeNoteFromHotRank(Integer noteId) {
        redisService.zRemove(RedisKey.noteHotRank(), String.valueOf(noteId));
    }

    private double calculateHotScore(Note note) {
        Integer noteId = note.getNoteId();

        Object likeCountObj = redisService.get(NOTE_LIKE_COUNT_KEY + noteId);
        if (likeCountObj != null) {
            note.setLikeCount(Integer.valueOf(String.valueOf(likeCountObj)));
        }

        Object collectCountObj = redisService.get(NOTE_COLLECT_COUNT_KEY + noteId);
        if (collectCountObj != null) {
            note.setCollectCount(Integer.valueOf(String.valueOf(collectCountObj)));
        }

        Object commentCountObj = redisService.get(NOTE_COMMENT_COUNT_KEY + noteId);
        if (commentCountObj != null) {
            note.setCommentCount(Integer.valueOf(String.valueOf(commentCountObj)));
        }

        return note.getLikeCount() * 2.0
                + note.getCommentCount() * 3.0
                + note.getCollectCount() * 4.0;
    }
}
