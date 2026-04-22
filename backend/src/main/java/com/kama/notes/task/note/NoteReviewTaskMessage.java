package com.kama.notes.task.note;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoteReviewTaskMessage {
    private Integer noteId;
    private Long userId;
}
