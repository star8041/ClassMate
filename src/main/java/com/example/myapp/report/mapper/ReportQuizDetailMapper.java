package com.example.myapp.report.mapper;

import com.example.myapp.report.dto.QuizAnswerDetail;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public class ReportQuizDetailMapper {

    private final JdbcClient jdbcClient;

    public ReportQuizDetailMapper(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    private static final RowMapper<QuizAnswerDetail> ROW_MAPPER = (rs, rowNum) -> new QuizAnswerDetail(
            rs.getLong("quiz_attempt_id"),
            rs.getLong("quiz_id"),
            rs.getString("quiz_title"),
            getNullableInt(rs, "score"),
            getNullableInt(rs, "correct_count"),
            getNullableInt(rs, "total_count"),
            rs.getTimestamp("submitted_at") != null
                    ? rs.getTimestamp("submitted_at").toLocalDateTime()
                    : null,

            rs.getLong("quiz_question_id"),
            getNullableInt(rs, "question_order"),
            rs.getString("question_text"),
            rs.getString("options"),
            rs.getString("student_answer"),
            rs.getString("correct_answer"),
            rs.getString("explanation"),
            rs.getBoolean("is_correct")
    );

    public List<QuizAnswerDetail> findByStudentIdAndPeriod(
            Long studentId,
            LocalDate periodStart,
            LocalDate periodEnd
    ) {
        return jdbcClient.sql("""
                SELECT
                    qa.quiz_attempt_id,
                    q.quiz_id,
                    q.title AS quiz_title,
                    qa.score,
                    qa.correct_count,
                    qa.total_count,
                    qa.submitted_at,

                    qq.quiz_question_id,
                    qq.question_order,
                    qq.question_text,
                    qq.options,
                    qq.answer_text AS correct_answer,
                    qq.explanation,

                    qans.answer_text AS student_answer,
                    qans.is_correct
                FROM quiz_attempt qa
                JOIN quiz q
                    ON q.quiz_id = qa.quiz_id
                JOIN quiz_answer qans
                    ON qans.quiz_attempt_id = qa.quiz_attempt_id
                JOIN quiz_question qq
                    ON qq.quiz_question_id = qans.quiz_question_id
                WHERE qa.student_id = ?
                  AND qa.submitted_at >= ?::date
                  AND qa.submitted_at < ?::date + interval '1 day'
                ORDER BY qa.submitted_at DESC, qq.question_order ASC
                """)
                .param(studentId)
                .param(periodStart)
                .param(periodEnd)
                .query(ROW_MAPPER)
                .list();
    }

    private static Integer getNullableInt(java.sql.ResultSet rs, String columnName) throws java.sql.SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }
}
