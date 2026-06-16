package com.example.myapp.quiz.mapper;

import com.example.myapp.quiz.entity.QuizAttempt;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * quiz_attempt 테이블 접근 매퍼.
 * 리포트 생성 시 학생의 기간별 응시 결과를 조회한다.
 */
@Repository
public class QuizAttemptMapper {

    private final JdbcClient jdbcClient;

    public QuizAttemptMapper(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    private static final RowMapper<QuizAttempt> ROW_MAPPER = (rs, rowNum) -> QuizAttempt.builder()
            .quizAttemptId(rs.getLong("quiz_attempt_id"))
            .quizId(rs.getLong("quiz_id"))
            .quizTitle(rs.getString("quiz_title"))
            .studentId(rs.getLong("student_id"))
            .score(rs.getObject("score", Integer.class))
            .totalCount(rs.getObject("total_count", Integer.class))
            .correctCount(rs.getObject("correct_count", Integer.class))
            .startedAt(rs.getTimestamp("started_at") != null
                    ? rs.getTimestamp("started_at").toLocalDateTime() : null)
            .submittedAt(rs.getTimestamp("submitted_at") != null
                    ? rs.getTimestamp("submitted_at").toLocalDateTime() : null)
            .build();

    /**
     * 학생의 기간 내 제출 완료된 응시 기록을 조회한다.
     * quiz 테이블 JOIN 으로 퀴즈 제목도 함께 가져온다.
     */
    public List<QuizAttempt> findByStudentIdAndPeriod(Long studentId,
                                                       LocalDate periodStart,
                                                       LocalDate periodEnd) {
        return jdbcClient.sql("""
                SELECT qa.*, q.title AS quiz_title
                FROM quiz_attempt qa
                JOIN quiz q ON qa.quiz_id = q.quiz_id
                WHERE qa.student_id = ?
                  AND qa.submitted_at >= ?
                  AND qa.submitted_at < ?
                ORDER BY qa.submitted_at
                """)
                .param(studentId)
                .param(periodStart.atStartOfDay())
                .param(periodEnd.plusDays(1).atStartOfDay())
                .query(ROW_MAPPER)
                .list();
    }
}
