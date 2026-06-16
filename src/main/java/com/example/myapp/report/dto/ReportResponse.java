package com.example.myapp.report.dto;

import com.example.myapp.report.entity.AchievementReport;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReportResponse(
        Long achievementReportId,
        Long studentId,
        LocalDate periodStart,
        LocalDate periodEnd,
        String summaryText,
        String strengthText,
        String weaknessText,
        String commentText,
        LocalDateTime createdAt
) {
    public static ReportResponse from(AchievementReport report) {
        return new ReportResponse(
                report.getAchievementReportId(),
                report.getStudentId(),
                report.getPeriodStart(),
                report.getPeriodEnd(),
                report.getSummaryText(),
                report.getStrengthText(),
                report.getWeaknessText(),
                report.getCommentText(),
                report.getCreatedAt()
        );
    }
}
