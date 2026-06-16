package com.example.myapp.schedule.dto;

import java.time.LocalDateTime;

/**
 * 일정 등록 요청 DTO.
 * <p>
 * teacherId 는 인증 연동 전까지 본문으로 받는다 (TODO: JWT 의 현재 교사로 대체).
 * status 미지정 시 SCHEDULED 로 저장한다.
 */
public record ScheduleCreateRequest(
        Long teacherId,
        Long studentId,
        String scheduleType,   // COUNSELING / CLASS / ETC
        String title,
        String topic,
        LocalDateTime scheduledAt,
        String status,
        String calendarEventId
) {
}
