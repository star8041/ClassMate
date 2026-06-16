package com.example.myapp.teacher.mapper;

import com.example.myapp.teacher.entity.Teacher;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * teacher 테이블 접근 매퍼 (JdbcClient).
 */
@Repository
public class TeacherMapper {

    private final JdbcClient jdbcClient;

    public TeacherMapper(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    private static final RowMapper<Teacher> ROW_MAPPER = (rs, rowNum) -> Teacher.builder()
            .teacherId(rs.getLong("teacher_id"))
            .loginId(rs.getString("login_id"))
            .password(rs.getString("password"))
            .teacherName(rs.getString("teacher_name"))
            .email(rs.getString("email"))
            .question(rs.getString("question"))
            .answer(rs.getString("answer"))
            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
            .updatedAt(rs.getTimestamp("updated_at") == null
                    ? null : rs.getTimestamp("updated_at").toLocalDateTime())
            .build();

    /** 회원가입: 새 교사를 저장하고 생성된 PK를 반환한다. */
    public Long insert(Teacher teacher) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql("""
                        INSERT INTO teacher (login_id, password, teacher_name, email, question, answer)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """)
                .param(teacher.getLoginId())
                .param(teacher.getPassword())
                .param(teacher.getTeacherName())
                .param(teacher.getEmail())
                .param(teacher.getQuestion())
                .param(teacher.getAnswer())
                .update(keyHolder, "teacher_id");
        return keyHolder.getKey().longValue();
    }

    /** 로그인 ID로 조회 */
    public Optional<Teacher> findByLoginId(String loginId) {
        return jdbcClient.sql("SELECT * FROM teacher WHERE login_id = ?")
                .param(loginId)
                .query(ROW_MAPPER)
                .optional();
    }

    /** PK로 조회 */
    public Optional<Teacher> findById(Long teacherId) {
        return jdbcClient.sql("SELECT * FROM teacher WHERE teacher_id = ?")
                .param(teacherId)
                .query(ROW_MAPPER)
                .optional();
    }

    /** 전체 교사 목록 (관리자용) */
    public java.util.List<Teacher> findAll() {
        return jdbcClient.sql("SELECT * FROM teacher ORDER BY teacher_id")
                .query(ROW_MAPPER)
                .list();
    }

    /** 로그인 ID 중복 여부 */
    public boolean existsByLoginId(String loginId) {
        Integer count = jdbcClient.sql("SELECT COUNT(*) FROM teacher WHERE login_id = ?")
                .param(loginId)
                .query(Integer.class)
                .single();
        return count != null && count > 0;
    }
}
