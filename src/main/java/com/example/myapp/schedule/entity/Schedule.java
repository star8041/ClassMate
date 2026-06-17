package com.example.myapp.schedule.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 교사 일정. schedule 테이블과 매핑.
 * scheduleType 은 UI 라벨(수업/상담/회의/수행평가/개인)을 그대로 저장한다.
 * 상담(상담) 유형일 때만 studentName/parentName/topic 을 사용한다.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Schedule {

    private Long scheduleId;
    private Long teacherId;
    private String scheduleType;   // 수업/상담/회의/수행평가/개인
    private String title;
    private String topic;          // 상담 주제 (상담 유형)
    private LocalDateTime scheduledAt;  // 시작 일시
    private LocalDateTime endAt;        // 종료 일시 (선택)
    private String location;       // 장소
    private String memo;           // 메모
    private String studentName;    // 상담 대상 학생명 (상담 유형)
    private String parentName;     // 학부모명 (상담 유형)
    private String status;         // SCHEDULED 등
    private LocalDateTime createdAt;
}
