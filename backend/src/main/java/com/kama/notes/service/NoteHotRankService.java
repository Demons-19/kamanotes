package com.kama.notes.service;

/**
 * 笔记热度排名服务
 */
public interface NoteHotRankService {
    /**
     * 更新笔记热度分数
     * @param noteId 笔记ID
     */
    void updateNoteHotScore(Integer noteId);

    /**
     * 从热度排行榜中移除笔记
     * @param noteId 笔记ID
     */
    void removeNoteFromHotRank(Integer noteId);
}
