package com.example.myapp.schedule.dto;

import java.time.LocalDateTime;

/**
 * 일정 수정(PATCH) 요청 DTO. 전달된(null 이 아닌) 필드만 변경한다.
 */
public record ScheduleUpdateRequest(
        Long studentId,
        String scheduleType,
        String title,
        String topic,
        LocalDateTime scheduledAt,
        String status,
        String calendarEventId
) {
}
