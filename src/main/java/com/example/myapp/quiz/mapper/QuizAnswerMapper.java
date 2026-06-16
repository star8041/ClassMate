package com.example.myapp.quiz.mapper;

import com.example.myapp.quiz.entity.QuizAnswer;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * quiz_answer 테이블 접근 매퍼.
 */
@Repository
public class QuizAnswerMapper {

    private final JdbcClient jdbcClient;

    public QuizAnswerMapper(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    private static final RowMapper<QuizAnswer> ROW_MAPPER = (rs, rowNum) -> QuizAnswer.builder()
            .quizAnswerId(rs.getLong("quiz_answer_id"))
            .quizAttemptId(rs.getLong("quiz_attempt_id"))
            .quizQuestionId(rs.getLong("quiz_question_id"))
            .answerText(rs.getString("answer_text"))
            .isCorrect(rs.getObject("is_correct", Boolean.class))
            .build();

    /** 특정 응시의 오답 목록 조회 */
    public List<QuizAnswer> findIncorrectByAttemptId(Long attemptId) {
        return jdbcClient.sql("""
                SELECT * FROM quiz_answer
                WHERE quiz_attempt_id = ? AND is_correct = false
                """)
                .param(attemptId)
                .query(ROW_MAPPER)
                .list();
    }

    /** 특정 응시의 전체 답변 조회 */
    public List<QuizAnswer> findByAttemptId(Long attemptId) {
        return jdbcClient.sql("""
                SELECT * FROM quiz_answer
                WHERE quiz_attempt_id = ?
                ORDER BY quiz_question_id
                """)
                .param(attemptId)
                .query(ROW_MAPPER)
                .list();
    }
}
