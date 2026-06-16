package com.example.myapp.schedule.dto;

import com.example.myapp.schedule.entity.Schedule;

import java.time.LocalDateTime;

/**
 * 일정 조회 응답 DTO.
 */
public record ScheduleResponse(
        Long scheduleId,
        Long teacherId,
        Long studentId,
        String scheduleType,
        String title,
        String topic,
        LocalDateTime scheduledAt,
        String status,
        String calendarEventId,
        LocalDateTime createdAt
) {
    public static ScheduleResponse from(Schedule s) {
        return new ScheduleResponse(
                s.getScheduleId(),
                s.getTeacherId(),
                s.getStudentId(),
                s.getScheduleType(),
                s.getTitle(),
                s.getTopic(),
                s.getScheduledAt(),
                s.getStatus(),
                s.getCalendarEventId(),
                s.getCreatedAt()
        );
    }
}
