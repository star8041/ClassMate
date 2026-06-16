package com.example.myapp.quiz.repository;

import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.example.myapp.quiz.domain.Quiz;
import com.example.myapp.quiz.domain.QuizAnswer;
import com.example.myapp.quiz.domain.QuizAttempt;
import com.example.myapp.quiz.domain.QuizQuestion;
import com.example.myapp.quiz.dto.QuestionUpdateRequest;
import com.example.myapp.quiz.dto.QuizAnswerResultResponse;
import com.example.myapp.quiz.dto.QuizListResponse;
import com.example.myapp.quiz.dto.QuizUpdateRequest;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class QuizRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public void insertQuiz(Quiz quiz) {
        String sql = """
                INSERT INTO quiz (
                    teacher_id,
                    material_id,
                    title,
                    difficulty,
                    start_page,
                    end_page,
                    available_from,
                    available_until
                )
                VALUES (
                    :teacherId,
                    :materialId,
                    :title,
                    :difficulty,
                    :startPage,
                    :endPage,
                    :availableFrom,
                    :availableUntil
                )
                RETURNING quiz_id
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("teacherId", quiz.getTeacherId())
                .addValue("materialId", quiz.getMaterialId())
                .addValue("title", quiz.getTitle())
                .addValue("difficulty", quiz.getDifficulty())
                .addValue("startPage", quiz.getStartPage())
                .addValue("endPage", quiz.getEndPage())
                .addValue("availableFrom", quiz.getAvailableFrom())
                .addValue("availableUntil", quiz.getAvailableUntil());

        Long quizId = jdbcTemplate.queryForObject(sql, params, Long.class);
        quiz.setQuizId(quizId);
    }

    public void insertQuizQuestion(QuizQuestion question) {
        String sql = """
                INSERT INTO quiz_question (
                    quiz_id,
                    question_type,
                    question_text,
                    options,
                    answer_text,
                    explanation,
                    question_order
                )
                VALUES (
                    :quizId,
                    :questionType,
                    :questionText,
                    CAST(:options AS jsonb),
                    :answerText,
                    :explanation,
                    :questionOrder
                )
                RETURNING quiz_question_id
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("quizId", question.getQuizId())
                .addValue("questionType", question.getQuestionType())
                .addValue("questionText", question.getQuestionText())
                .addValue("options", question.getOptions())
                .addValue("answerText", question.getAnswerText())
                .addValue("explanation", question.getExplanation())
                .addValue("questionOrder", question.getQuestionOrder());

        Long questionId = jdbcTemplate.queryForObject(sql, params, Long.class);
        question.setQuizQuestionId(questionId);
    }

    public List<QuizListResponse> findQuizList() {
        String sql = """
                SELECT
                    q.quiz_id,
                    q.teacher_id,
                    q.material_id,
                    q.title,
                    q.difficulty,
                    q.start_page,
                    q.end_page,
                    q.available_from,
                    q.available_until,
                    q.created_at,
                    COUNT(qq.quiz_question_id) AS question_count
                FROM quiz q
                LEFT JOIN quiz_question qq
                    ON q.quiz_id = qq.quiz_id
                GROUP BY
                    q.quiz_id,
                    q.teacher_id,
                    q.material_id,
                    q.title,
                    q.difficulty,
                    q.start_page,
                    q.end_page,
                    q.available_from,
                    q.available_until,
                    q.created_at
                ORDER BY q.quiz_id DESC
                """;

        return jdbcTemplate.query(sql, new MapSqlParameterSource(), quizListRowMapper());
    }

    public Quiz findQuizById(Long quizId) {
        String sql = """
                SELECT
                    quiz_id,
                    teacher_id,
                    material_id,
                    title,
                    difficulty,
                    start_page,
                    end_page,
                    available_from,
                    available_until,
                    created_at
                FROM quiz
                WHERE quiz_id = :quizId
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("quizId", quizId);

        List<Quiz> result = jdbcTemplate.query(sql, params, quizRowMapper());
        return result.isEmpty() ? null : result.get(0);
    }

    public List<QuizQuestion> findQuestionsByQuizId(Long quizId) {
        String sql = """
                SELECT
                    quiz_question_id,
                    quiz_id,
                    question_type,
                    question_text,
                    options::text AS options,
                    answer_text,
                    explanation,
                    question_order
                FROM quiz_question
                WHERE quiz_id = :quizId
                ORDER BY question_order ASC
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("quizId", quizId);

        return jdbcTemplate.query(sql, params, quizQuestionRowMapper());
    }

    public int updateQuiz(Long quizId, QuizUpdateRequest request) {
        List<String> sets = new ArrayList<>();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("quizId", quizId);

        if (request.getTitle() != null) {
            sets.add("title = :title");
            params.addValue("title", request.getTitle());
        }

        if (request.getDifficulty() != null) {
            sets.add("difficulty = :difficulty");
            params.addValue("difficulty", request.getDifficulty());
        }

        if (request.getStartPage() != null) {
            sets.add("start_page = :startPage");
            params.addValue("startPage", request.getStartPage());
        }

        if (request.getEndPage() != null) {
            sets.add("end_page = :endPage");
            params.addValue("endPage", request.getEndPage());
        }

        if (sets.isEmpty()) {
            return 0;
        }

        String sql = """
                UPDATE quiz
                SET %s
                WHERE quiz_id = :quizId
                """.formatted(String.join(", ", sets));

        return jdbcTemplate.update(sql, params);
    }

    public int deleteQuiz(Long quizId) {
        String sql = """
                DELETE FROM quiz
                WHERE quiz_id = :quizId
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("quizId", quizId);

        return jdbcTemplate.update(sql, params);
    }

    public int updateQuestion(
            Long quizId,
            Long questionId,
            QuestionUpdateRequest request
    ) {
        List<String> sets = new ArrayList<>();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("quizId", quizId)
                .addValue("questionId", questionId);

        if (request.getQuestionType() != null) {
            sets.add("question_type = :questionType");
            params.addValue("questionType", request.getQuestionType());
        }

        if (request.getQuestionText() != null) {
            sets.add("question_text = :questionText");
            params.addValue("questionText", request.getQuestionText());
        }

        if (request.getOptions() != null) {
            sets.add("options = CAST(:options AS jsonb)");
            params.addValue("options", request.getOptions());
        }

        if (request.getAnswerText() != null) {
            sets.add("answer_text = :answerText");
            params.addValue("answerText", request.getAnswerText());
        }

        if (request.getExplanation() != null) {
            sets.add("explanation = :explanation");
            params.addValue("explanation", request.getExplanation());
        }

        if (request.getQuestionOrder() != null) {
            sets.add("question_order = :questionOrder");
            params.addValue("questionOrder", request.getQuestionOrder());
        }

        if (sets.isEmpty()) {
            return 0;
        }

        String sql = """
                UPDATE quiz_question
                SET %s
                WHERE quiz_id = :quizId
                  AND quiz_question_id = :questionId
                """.formatted(String.join(", ", sets));

        return jdbcTemplate.update(sql, params);
    }

    public void insertQuizAttempt(QuizAttempt attempt) {
        String sql = """
                INSERT INTO quiz_attempt (
                    quiz_id,
                    student_id,
                    score,
                    total_count,
                    correct_count
                )
                VALUES (
                    :quizId,
                    :studentId,
                    0,
                    0,
                    0
                )
                RETURNING quiz_attempt_id
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("quizId", attempt.getQuizId())
                .addValue("studentId", attempt.getStudentId());

        Long attemptId = jdbcTemplate.queryForObject(sql, params, Long.class);
        attempt.setQuizAttemptId(attemptId);
    }

    public QuizAttempt findAttemptById(Long quizId, Long attemptId) {
        String sql = """
                SELECT
                    quiz_attempt_id,
                    quiz_id,
                    student_id,
                    score,
                    total_count,
                    correct_count,
                    started_at,
                    submitted_at
                FROM quiz_attempt
                WHERE quiz_id = :quizId
                  AND quiz_attempt_id = :attemptId
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("quizId", quizId)
                .addValue("attemptId", attemptId);

        List<QuizAttempt> result = jdbcTemplate.query(sql, params, quizAttemptRowMapper());
        return result.isEmpty() ? null : result.get(0);
    }

    public void insertQuizAnswer(QuizAnswer answer) {
        String sql = """
                INSERT INTO quiz_answer (
                    quiz_attempt_id,
                    quiz_question_id,
                    answer_text,
                    is_correct
                )
                VALUES (
                    :quizAttemptId,
                    :quizQuestionId,
                    :answerText,
                    :isCorrect
                )
                RETURNING quiz_answer_id
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("quizAttemptId", answer.getQuizAttemptId())
                .addValue("quizQuestionId", answer.getQuizQuestionId())
                .addValue("answerText", answer.getAnswerText())
                .addValue("isCorrect", answer.getIsCorrect());

        Long answerId = jdbcTemplate.queryForObject(sql, params, Long.class);
        answer.setQuizAnswerId(answerId);
    }

    public int updateAttemptResult(
            Long attemptId,
            int score,
            int totalCount,
            int correctCount
    ) {
        String sql = """
                UPDATE quiz_attempt
                SET
                    score = :score,
                    total_count = :totalCount,
                    correct_count = :correctCount,
                    submitted_at = CURRENT_TIMESTAMP
                WHERE quiz_attempt_id = :attemptId
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("attemptId", attemptId)
                .addValue("score", score)
                .addValue("totalCount", totalCount)
                .addValue("correctCount", correctCount);

        return jdbcTemplate.update(sql, params);
    }

    public List<QuizAnswerResultResponse> findAnswerResultsByAttemptId(Long attemptId) {
        String sql = """
                SELECT
                    qq.quiz_question_id,
                    qq.question_text,
                    qa.answer_text,
                    qq.answer_text AS correct_answer,
                    qa.is_correct,
                    qq.explanation
                FROM quiz_answer qa
                JOIN quiz_question qq
                    ON qa.quiz_question_id = qq.quiz_question_id
                WHERE qa.quiz_attempt_id = :attemptId
                ORDER BY qq.question_order ASC
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("attemptId", attemptId);

        return jdbcTemplate.query(sql, params, answerResultRowMapper());
    }

    private RowMapper<Quiz> quizRowMapper() {
        return (rs, rowNum) -> {
            Quiz quiz = new Quiz();
            quiz.setQuizId(rs.getLong("quiz_id"));
            quiz.setTeacherId(rs.getLong("teacher_id"));

            Long materialId = rs.getObject("material_id", Long.class);
            quiz.setMaterialId(materialId);

            quiz.setTitle(rs.getString("title"));
            quiz.setDifficulty(rs.getString("difficulty"));

            Integer startPage = rs.getObject("start_page", Integer.class);
            Integer endPage = rs.getObject("end_page", Integer.class);
            quiz.setStartPage(startPage);
            quiz.setEndPage(endPage);

            Timestamp availableFrom = rs.getTimestamp("available_from");
            Timestamp availableUntil = rs.getTimestamp("available_until");
            Timestamp createdAt = rs.getTimestamp("created_at");

            quiz.setAvailableFrom(availableFrom == null ? null : availableFrom.toLocalDateTime());
            quiz.setAvailableUntil(availableUntil == null ? null : availableUntil.toLocalDateTime());
            quiz.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());

            return quiz;
        };
    }

    private RowMapper<QuizQuestion> quizQuestionRowMapper() {
        return (rs, rowNum) -> {
            QuizQuestion question = new QuizQuestion();
            question.setQuizQuestionId(rs.getLong("quiz_question_id"));
            question.setQuizId(rs.getLong("quiz_id"));
            question.setQuestionType(rs.getString("question_type"));
            question.setQuestionText(rs.getString("question_text"));
            question.setOptions(rs.getString("options"));
            question.setAnswerText(rs.getString("answer_text"));
            question.setExplanation(rs.getString("explanation"));
            question.setQuestionOrder(rs.getObject("question_order", Integer.class));
            return question;
        };
    }

    private RowMapper<QuizAttempt> quizAttemptRowMapper() {
        return (rs, rowNum) -> {
            QuizAttempt attempt = new QuizAttempt();
            attempt.setQuizAttemptId(rs.getLong("quiz_attempt_id"));
            attempt.setQuizId(rs.getLong("quiz_id"));
            attempt.setStudentId(rs.getLong("student_id"));
            attempt.setScore(rs.getObject("score", Integer.class));
            attempt.setTotalCount(rs.getObject("total_count", Integer.class));
            attempt.setCorrectCount(rs.getObject("correct_count", Integer.class));

            Timestamp startedAt = rs.getTimestamp("started_at");
            Timestamp submittedAt = rs.getTimestamp("submitted_at");

            attempt.setStartedAt(startedAt == null ? null : startedAt.toLocalDateTime());
            attempt.setSubmittedAt(submittedAt == null ? null : submittedAt.toLocalDateTime());

            return attempt;
        };
    }

    private RowMapper<QuizListResponse> quizListRowMapper() {
        return (rs, rowNum) -> {
            QuizListResponse response = new QuizListResponse();
            response.setQuizId(rs.getLong("quiz_id"));
            response.setTeacherId(rs.getLong("teacher_id"));
            response.setMaterialId(rs.getObject("material_id", Long.class));
            response.setTitle(rs.getString("title"));
            response.setDifficulty(rs.getString("difficulty"));
            response.setStartPage(rs.getObject("start_page", Integer.class));
            response.setEndPage(rs.getObject("end_page", Integer.class));

            Timestamp availableFrom = rs.getTimestamp("available_from");
            Timestamp availableUntil = rs.getTimestamp("available_until");
            Timestamp createdAt = rs.getTimestamp("created_at");

            response.setAvailableFrom(
                    availableFrom == null ? null : availableFrom.toLocalDateTime()
            );
            response.setAvailableUntil(
                    availableUntil == null ? null : availableUntil.toLocalDateTime()
            );
            response.setCreatedAt(
                    createdAt == null ? null : createdAt.toLocalDateTime()
            );

            Number questionCount = (Number) rs.getObject("question_count");
            response.setQuestionCount(questionCount == null ? 0 : questionCount.intValue());

            return response;
        };
    }

    private RowMapper<QuizAnswerResultResponse> answerResultRowMapper() {
        return (rs, rowNum) -> {
            QuizAnswerResultResponse response = new QuizAnswerResultResponse();
            response.setQuizQuestionId(rs.getLong("quiz_question_id"));
            response.setQuestionText(rs.getString("question_text"));
            response.setAnswerText(rs.getString("answer_text"));
            response.setCorrectAnswer(rs.getString("correct_answer"));
            response.setIsCorrect(rs.getBoolean("is_correct"));
            response.setExplanation(rs.getString("explanation"));
            return response;
        };
    }
}