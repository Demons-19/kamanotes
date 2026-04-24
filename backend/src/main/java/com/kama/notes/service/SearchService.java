package com.kama.notes.service;

import com.kama.notes.model.base.ApiResponse;
import com.kama.notes.model.entity.Note;
import com.kama.notes.model.entity.User;
import com.kama.notes.model.vo.note.NoteVO;

import java.util.List;

public interface SearchService {
    /**
     * 搜索笔记
     *
     * @param keyword 关键词
     * @param page 页码
     * @param pageSize 每页大小
     * @return 笔记列表
     */
    ApiResponse<List<Note>> searchNotes(String keyword, int page, int pageSize);

    /**
     * 按问题标题 + 笔记内容全文检索
     *
     * @param keyword 关键词
     * @param page 页码
     * @param pageSize 每页大小
     * @return 笔记列表
     */
    ApiResponse<List<NoteVO>> searchNotesFullText(String keyword, int page, int pageSize);

    /**
     * 搜索用户
     *
     * @param keyword 关键词
     * @param page 页码
     * @param pageSize 每页大小
     * @return 用户列表
     */
    ApiResponse<List<User>> searchUsers(String keyword, int page, int pageSize);

    /**
     * 搜索笔记（带标签）
     *
     * @param keyword 关键词
     * @param tag 标签
     * @param page 页码
     * @param pageSize 每页大小
     * @return 笔记列表
     */
    ApiResponse<List<Note>> searchNotesByTag(String keyword, String tag, int page, int pageSize);

    /**
     * 按问题标题 + 笔记内容模糊匹配搜索
     *
     * @param keyword 关键词
     * @param page 页码
     * @param pageSize 每页大小
     * @return 笔记列表
     */
    ApiResponse<List<NoteVO>> searchNotesByLike(String keyword, int page, int pageSize);
} 