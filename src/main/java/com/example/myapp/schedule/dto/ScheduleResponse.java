package com.example.myapp.schedule.dto;

import com.example.myapp.schedule.entity.Schedule;

import java.time.LocalDateTime;

public record ScheduleResponse(
        Long scheduleId,
        Long teacherId,
        Long studentId,
        String scheduleType,
        String title,
        String topic,
        LocalDateTime scheduledAt,
        LocalDateTime endAt,
        String location,
        String memo,
        String studentName,
        String parentName,
        String status
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
                s.getEndAt(),
                s.getLocation(),
                s.getMemo(),
                s.getStudentName(),
                s.getParentName(),
                s.getStatus()
        );
    }
}
