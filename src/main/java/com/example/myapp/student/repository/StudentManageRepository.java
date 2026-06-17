package com.example.myapp.student.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.myapp.student.dto.StudentListResponse;
import com.example.myapp.student.dto.StudentQuizResultResponse;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class StudentManageRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<StudentListResponse> findStudentsByTeacherId(Long teacherId) {
        String sql = """
            SELECT
                s.student_id,
                s.student_name,
                s.student_number
            FROM student s
            WHERE s.teacher_id = :teacherId
            ORDER BY s.student_number ASC, s.student_name ASC
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("teacherId", teacherId);

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            StudentListResponse response = new StudentListResponse();

            response.setStudentId(rs.getLong("student_id"));
            response.setStudentName(rs.getString("student_name"));
            response.setStudentNumber(rs.getString("student_number"));

            return response;
        });
    }

    public Optional<StudentListResponse> findStudentByIdAndTeacherId(Long studentId, Long teacherId) {
        String sql = """
            SELECT
                s.student_id,
                s.student_name,
                s.student_number
            FROM student s
            WHERE s.student_id = :studentId
              AND s.teacher_id = :teacherId
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("studentId", studentId)
                .addValue("teacherId", teacherId);

        List<StudentListResponse> result = jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            StudentListResponse response = new StudentListResponse();

            response.setStudentId(rs.getLong("student_id"));
            response.setStudentName(rs.getString("student_name"));
            response.setStudentNumber(rs.getString("student_number"));

            return response;
        });

        return result.stream().findFirst();
    }

    public List<StudentQuizResultResponse> findQuizResultsByStudentId(Long studentId, Long teacherId) {
        String sql = """
            SELECT
                q.quiz_id,
                q.title AS quiz_title,
                qa.quiz_attempt_id,
                qa.score,
                qa.total_count,
                qa.correct_count,
                qa.submitted_at
            FROM quiz_attempt qa
            JOIN quiz q
                ON qa.quiz_id = q.quiz_id
            JOIN student s
                ON qa.student_id = s.student_id
            WHERE qa.student_id = :studentId
              AND s.teacher_id = :teacherId
              AND q.teacher_id = :teacherId
              AND qa.submitted_at IS NOT NULL
            ORDER BY qa.submitted_at DESC
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("studentId", studentId)
                .addValue("teacherId", teacherId);

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            StudentQuizResultResponse response = new StudentQuizResultResponse();

            response.setQuizId(rs.getLong("quiz_id"));
            response.setAttemptId(rs.getLong("quiz_attempt_id"));
            response.setQuizTitle(rs.getString("quiz_title"));

            if (rs.getTimestamp("submitted_at") != null) {
                response.setSubmittedAt(rs.getTimestamp("submitted_at").toLocalDateTime());
            }

            response.setScore(rs.getObject("score", Integer.class));
            response.setTotalCount(rs.getObject("total_count", Integer.class));
            response.setCorrectCount(rs.getObject("correct_count", Integer.class));

            // AI 코멘트는 나중에 자동 생성할 예정이므로 현재는 비워둔다.
            response.setComment("");

            return response;
        });
    }

    public int countTeacherQuiz(Long teacherId) {
        String sql = """
            SELECT COUNT(*)
            FROM quiz
            WHERE teacher_id = :teacherId
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("teacherId", teacherId);

        Integer count = jdbcTemplate.queryForObject(sql, params, Integer.class);

        return count == null ? 0 : count;
    }
}
