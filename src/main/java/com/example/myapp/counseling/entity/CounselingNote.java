package com.example.myapp.counseling.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 상담 기록 엔티티. counseling_note 테이블과 1:1 매핑된다.
 * (상담 일정(schedule)에 연결된 녹음 원문/AI 요약/후속 조치)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CounselingNote {

    private Long counselingNoteId;
    private Long scheduleId;        // 연결된 상담 일정 (FK)
    private String rawText;         // 녹음 원문(STT 결과)
    private String summaryText;     // AI 요약
    private String followUpText;    // 후속 조치
    private LocalDateTime createdAt;
}
