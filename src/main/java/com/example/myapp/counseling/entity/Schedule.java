package com.example.myapp.counseling.entity;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class Schedule {
    private Long scheduleId;
    private Long teacherId;
    private Long studentId;
    private String scheduleType;
    private String title;
    private String topic;
    private LocalDateTime scheduledAt;
    private String status;
    private String calendarEventId;
    private LocalDateTime createdAt;
}
