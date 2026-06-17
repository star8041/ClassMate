package com.example.myapp.student.mapper;

import com.example.myapp.student.entity.Student;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * student 테이블 접근 매퍼.
 * <p>
 * 프로젝트 영속성 방침(JDBC)에 맞춰 Spring {@link JdbcClient}로 구현한다.
 * (material 도메인과 동일한 방식)
 */
@Repository
public class StudentMapper {

    private final JdbcClient jdbcClient;

    public StudentMapper(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /** student 행 → Student 엔티티 매핑 (snake_case → camelCase) */
    private static final RowMapper<Student> ROW_MAPPER = (rs, rowNum) -> Student.builder()
            .studentId(rs.getLong("student_id"))
            .teacherId(rs.getLong("teacher_id"))
            .studentName(rs.getString("student_name"))
            .studentNumber(rs.getString("student_number"))
            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
            .updatedAt(rs.getTimestamp("updated_at") == null
                    ? null : rs.getTimestamp("updated_at").toLocalDateTime())
            .build();

    /**
     * 새 학생을 저장하고 생성된 PK를 반환한다.
     */
    public Long insert(Student student) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql("""
                        INSERT INTO student (teacher_id, student_name, student_number)
                        VALUES (?, ?, ?)
                        """)
                .param(student.getTeacherId())
                .param(student.getStudentName())
                .param(student.getStudentNumber())
                .update(keyHolder, "student_id");
        return keyHolder.getKey().longValue();
    }

    /**
     * 전체 또는 특정 교사의 학생 목록을 이름순으로 조회한다.
     *
     * @param teacherId null 이면 전체 조회
     */
    public List<Student> findAll(Long teacherId) {
        if (teacherId == null) {
            return jdbcClient.sql("SELECT * FROM student ORDER BY student_name")
                    .query(ROW_MAPPER)
                    .list();
        }
        return jdbcClient.sql("SELECT * FROM student WHERE teacher_id = ? ORDER BY student_name")
                .param(teacherId)
                .query(ROW_MAPPER)
                .list();
    }

    /**
     * 이름 + 학번 + 담임교사 ID로 학생을 조회한다.
     * 학생 로그인 시 본인 확인에 사용한다.
     */
    public Optional<Student> findByNameAndStudentNumberAndTeacherId(
            String studentName, String studentNumber, Long teacherId) {
        return jdbcClient.sql("""
                        SELECT * FROM student
                         WHERE student_name = ?
                           AND student_number = ?
                           AND teacher_id = ?
                         LIMIT 1
                        """)
                .param(studentName)
                .param(studentNumber)
                .param(teacherId)
                .query(ROW_MAPPER)
                .optional();
    }

    /** 단건 조회 */
    public Optional<Student> findById(Long studentId) {
        return jdbcClient.sql("SELECT * FROM student WHERE student_id = ?")
                .param(studentId)
                .query(ROW_MAPPER)
                .optional();
    }

    /** 이름/학번 수정 (수정된 행 수 반환) */
    public int update(Student student) {
        return jdbcClient.sql("""
                        UPDATE student
                           SET student_name = ?, student_number = ?, updated_at = CURRENT_TIMESTAMP
                         WHERE student_id = ?
                        """)
                .param(student.getStudentName())
                .param(student.getStudentNumber())
                .param(student.getStudentId())
                .update();
    }

    /** 삭제 (삭제된 행 수 반환) */
    public int deleteById(Long studentId) {
        return jdbcClient.sql("DELETE FROM student WHERE student_id = ?")
                .param(studentId)
                .update();
    }

    /**
     * 이름 + 담임교사 ID로 학생 목록을 조회한다.
     * 동명이인이 있을 수 있으므로 List 반환.
     */
    public List<Student> findByNameAndTeacherId(String studentName, Long teacherId) {
        return jdbcClient.sql("""
                        SELECT * FROM student
                         WHERE student_name = ?
                           AND teacher_id = ?
                         ORDER BY student_number
                        """)
                .param(studentName)
                .param(teacherId)
                .query(ROW_MAPPER)
                .list();
    }
}
