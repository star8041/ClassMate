package com.example.myapp.counseling.dto;

/**
 * 상담 녹음 → STT 변환 + 자동 요약 결과.
 */
public record TranscriptionResponse(
        String transcript,
        String summary,
        int charCount
) {
}
