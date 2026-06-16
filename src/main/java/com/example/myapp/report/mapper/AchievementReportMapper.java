package com.example.myapp.report.mapper;

import com.example.myapp.report.entity.AchievementReport;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * achievement_report 테이블 접근 매퍼 (JdbcClient).
 */
@Repository
public class AchievementReportMapper {

    private final JdbcClient jdbcClient;

    public AchievementReportMapper(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    private static final RowMapper<AchievementReport> ROW_MAPPER = (rs, rowNum) -> AchievementReport.builder()
            .achievementReportId(rs.getLong("achievement_report_id"))
            .studentId(rs.getLong("student_id"))
            .periodStart(rs.getDate("period_start").toLocalDate())
            .periodEnd(rs.getDate("period_end").toLocalDate())
            .summaryText(rs.getString("summary_text"))
            .strengthText(rs.getString("strength_text"))
            .weaknessText(rs.getString("weakness_text"))
            .commentText(rs.getString("comment_text"))
            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
            .build();

    /** 리포트를 저장하고 생성된 PK를 반환한다. */
    public Long insert(AchievementReport report) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql("""
                INSERT INTO achievement_report
                    (student_id, period_start, period_end,
                     summary_text, strength_text, weakness_text, comment_text)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """)
                .param(report.getStudentId())
                .param(report.getPeriodStart())
                .param(report.getPeriodEnd())
                .param(report.getSummaryText())
                .param(report.getStrengthText())
                .param(report.getWeaknessText())
                .param(report.getCommentText())
                .update(keyHolder, "achievement_report_id");
        return keyHolder.getKey().longValue();
    }

    /** 단건 조회 */
    public Optional<AchievementReport> findById(Long reportId) {
        return jdbcClient.sql("SELECT * FROM achievement_report WHERE achievement_report_id = ?")
                .param(reportId)
                .query(ROW_MAPPER)
                .optional();
    }

    /** 학생의 리포트 목록 (최신순) */
    public List<AchievementReport> findByStudentId(Long studentId) {
        return jdbcClient.sql("""
                SELECT * FROM achievement_report
                WHERE student_id = ?
                ORDER BY created_at DESC
                """)
                .param(studentId)
                .query(ROW_MAPPER)
                .list();
    }

    /** 단건 삭제 */
    public void deleteById(Long reportId) {
        jdbcClient.sql("DELETE FROM achievement_report WHERE achievement_report_id = ?")
                .param(reportId)
                .update();
    }
}
