package com.example.myapp.schedule.dto;

import java.time.LocalDateTime;

/**
 * 일정 등록 요청. 프런트 폼(유형/제목/날짜/시간/장소/메모 + 상담 추가필드)을 받는다.
 */
public record ScheduleCreateRequest(
        String scheduleType,
        String title,
        String topic,
        LocalDateTime scheduledAt,
        LocalDateTime endAt,
        String location,
        String memo,
        Long studentId,
        String studentName,
        String parentName
) {
}
