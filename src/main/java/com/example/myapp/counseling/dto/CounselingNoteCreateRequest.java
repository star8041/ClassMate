package com.example.myapp.counseling.dto;

/**
 * 상담 기록 등록 요청 DTO.
 * <p>
 * 녹음 STT/AI 요약은 별도 처리(AI Agent)이며, 이 API 는 결과 텍스트를 저장한다.
 * 텍스트 필드는 선택 — 등록 후 요약/후속조치를 채울 수 있다.
 */
public record CounselingNoteCreateRequest(
        Long scheduleId,
        String rawText,
        String summaryText,
        String followUpText
) {
}
