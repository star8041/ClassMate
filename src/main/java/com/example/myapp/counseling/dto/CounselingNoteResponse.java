package com.example.myapp.counseling.dto;

import com.example.myapp.counseling.entity.CounselingNote;

import java.time.LocalDateTime;

/**
 * 상담 기록 조회 응답 DTO.
 */
public record CounselingNoteResponse(
        Long counselingNoteId,
        Long scheduleId,
        String rawText,
        String summaryText,
        String followUpText,
        LocalDateTime createdAt
) {
    public static CounselingNoteResponse from(CounselingNote note) {
        return new CounselingNoteResponse(
                note.getCounselingNoteId(),
                note.getScheduleId(),
                note.getRawText(),
                note.getSummaryText(),
                note.getFollowUpText(),
                note.getCreatedAt()
        );
    }
}
