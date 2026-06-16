package com.example.myapp.schedule.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 교사의 일정(상담/수업/기타) 엔티티. schedule 테이블과 1:1 매핑된다.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Schedule {

    private Long scheduleId;
    private Long teacherId;
    private Long studentId;          // COUNSELING 인 경우 사용 (선택)
    private String scheduleType;     // COUNSELING / CLASS / ETC
    private String title;
    private String topic;            // 선택
    private LocalDateTime scheduledAt;
    private String status;           // SCHEDULED / ... (기본 SCHEDULED)
    private String calendarEventId;  // Google Calendar 연동용 (선택)
    private LocalDateTime createdAt;
}
