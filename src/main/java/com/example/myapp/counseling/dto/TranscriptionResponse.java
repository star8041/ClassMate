package com.example.myapp.counseling.dto;

/**
 * 상담 녹음 → STT 변환 + 자동 요약 + 후속 조치 결과.
 */
public record TranscriptionResponse(
        String transcript,
        String summary,
        String followUp,
        int charCount
) {
}
