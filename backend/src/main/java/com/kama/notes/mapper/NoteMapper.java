package com.kama.notes.mapper;

import com.kama.notes.model.dto.note.NoteQueryParams;
import com.kama.notes.model.entity.Note;
import com.kama.notes.model.vo.note.NoteHeatMapItem;
import com.kama.notes.model.vo.note.NoteRankListItem;
import com.kama.notes.model.vo.note.Top3Count;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

@Mapper
public interface NoteMapper {
    int countNotes(@Param("params") NoteQueryParams params);

    Note findById(@Param("noteId") Integer noteId);

    Note findRawById(@Param("noteId") Integer noteId);

    List<Note> findByIdBatch(@Param("noteIds") List<Integer> noteIds);

    List<Note> findByQueryParams(@Param("params") NoteQueryParams params,
                                 @Param("offset") int offset,
                                 @Param("limit") int limit);

    Note findByAuthorIdAndQuestionId(@Param("authorId") Long authorId,
                                     @Param("questionId") Integer questionId);

    List<Note> findByAuthorId(@Param("authorId") Long authorId);

    Set<Integer> filterFinishedQuestionIdsByUser(@Param("authorId") Long authorId,
                                                 @Param("questionIds") List<Integer> questionIds);

    int insert(Note note);

    int update(Note note);

    int likeNote(@Param("noteId") Integer noteId);

    int unlikeNote(@Param("noteId") Integer noteId);

    int collectNote(@Param("noteId") Integer noteId);

    int unCollectNote(@Param("noteId") Integer noteId);

    int deleteById(@Param("noteId") Integer noteId);

    List<NoteRankListItem> submitNoteRank();

    List<NoteHeatMapItem> submitNoteHeatMap(@Param("authorId") Long authorId);

    Top3Count submitNoteTop3Count(@Param("authorId") Long authorId);

    int getTodayNoteCount();

    int getTodaySubmitNoteUserCount();

    int getTotalNoteCount();

    void incrementCommentCount(@Param("noteId") Integer noteId);

    void decrementCommentCount(@Param("noteId") Integer noteId);

    List<Note> searchNotes(@Param("keyword") String keyword,
                           @Param("limit") int limit,
                           @Param("offset") int offset);

    List<Note> searchNotesByTitleAndContent(@Param("keyword") String keyword,
                                            @Param("limit") int limit,
                                            @Param("offset") int offset);

    List<Note> searchNotesByTag(@Param("keyword") String keyword,
                                @Param("tag") String tag,
                                @Param("limit") int limit,
                                @Param("offset") int offset);

    List<Note> searchNotesByTitleAndContentLike(@Param("keyword") String keyword,
                                                @Param("limit") int limit,
                                                @Param("offset") int offset);

    int batchUpdateLikeCount(@Param("list") List<Note> notes);

    int batchUpdateCollectCount(@Param("list") List<Note> notes);

    int batchUpdateCommentCount(@Param("list") List<Note> notes);

    int updateStatus(@Param("noteId") Integer noteId, @Param("status") Integer status);

    int updateStatusIfCurrent(@Param("noteId") Integer noteId,
                              @Param("currentStatus") Integer currentStatus,
                              @Param("targetStatus") Integer targetStatus);

    int incrementReviewRetryCount(@Param("noteId") Integer noteId, @Param("reviewLastError") String reviewLastError);

    int markReviewFailed(@Param("noteId") Integer noteId, @Param("reviewLastError") String reviewLastError);

    int clearReviewError(@Param("noteId") Integer noteId);
}
