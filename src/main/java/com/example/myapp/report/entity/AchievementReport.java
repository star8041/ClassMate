package com.example.myapp.report.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * achievement_report 테이블 매핑 POJO.
 * 퀴즈 성취도 + 상담 기록을 종합하여 AI가 생성한 학생 지도 리포트.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AchievementReport {
    private Long achievementReportId;
    private Long studentId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String summaryText;
    private String strengthText;
    private String weaknessText;
    private String commentText;
    private LocalDateTime createdAt;
}
