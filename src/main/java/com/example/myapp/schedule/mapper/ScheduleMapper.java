package com.example.myapp.schedule.mapper;

import com.example.myapp.schedule.entity.Schedule;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

/**
 * schedule 테이블 접근 매퍼 (Spring JdbcClient).
 */
@Repository
public class ScheduleMapper {

    private final JdbcClient jdbcClient;

    public ScheduleMapper(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    private static final RowMapper<Schedule> ROW_MAPPER = (rs, rowNum) -> Schedule.builder()
            .scheduleId(rs.getLong("schedule_id"))
            .teacherId(rs.getLong("teacher_id"))
            .studentId(rs.getObject("student_id", Long.class))
            .scheduleType(rs.getString("schedule_type"))
            .title(rs.getString("title"))
            .topic(rs.getString("topic"))
            .scheduledAt(rs.getTimestamp("scheduled_at").toLocalDateTime())
            .endAt(rs.getTimestamp("end_at") != null ? rs.getTimestamp("end_at").toLocalDateTime() : null)
            .location(rs.getString("location"))
            .memo(rs.getString("memo"))
            .studentName(rs.getString("student_name"))
            .parentName(rs.getString("parent_name"))
            .status(rs.getString("status"))
            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
            .build();

    public Long insert(Schedule s) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql("""
                        INSERT INTO schedule
                            (teacher_id, student_id, schedule_type, title, topic, scheduled_at, end_at,
                             location, memo, student_name, parent_name, status)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """)
                .param(s.getTeacherId())
                .param(s.getStudentId())
                .param(s.getScheduleType())
                .param(s.getTitle())
                .param(s.getTopic())
                .param(Timestamp.valueOf(s.getScheduledAt()))
                .param(s.getEndAt() != null ? Timestamp.valueOf(s.getEndAt()) : null)
                .param(s.getLocation())
                .param(s.getMemo())
                .param(s.getStudentName())
                .param(s.getParentName())
                .param(s.getStatus() == null ? "SCHEDULED" : s.getStatus())
                .update(keyHolder, "schedule_id");
        return keyHolder.getKey().longValue();
    }

    /** 교사의 전체 일정 (최신순) */
    public List<Schedule> findByTeacher(Long teacherId) {
        return jdbcClient.sql("SELECT * FROM schedule WHERE teacher_id = ? ORDER BY scheduled_at")
                .param(teacherId)
                .query(ROW_MAPPER)
                .list();
    }

    /** 교사의 특정 유형 일정 (시간순) */
    public List<Schedule> findByTeacherAndType(Long teacherId, String type) {
        return jdbcClient.sql("SELECT * FROM schedule WHERE teacher_id = ? AND schedule_type = ? ORDER BY scheduled_at")
                .param(teacherId)
                .param(type)
                .query(ROW_MAPPER)
                .list();
    }

    /** 교사의 특정 날짜 일정 (시간순) */
    public List<Schedule> findByTeacherAndDate(Long teacherId, java.time.LocalDate date) {
        return jdbcClient.sql("""
                SELECT * FROM schedule
                WHERE teacher_id = ?
                  AND DATE(scheduled_at) = ?
                ORDER BY scheduled_at
                """)
                .param(teacherId)
                .param(date)
                .query(ROW_MAPPER)
                .list();
    }

    /** 교사의 특정 날짜 + 유형 일정 */
    public List<Schedule> findByTeacherAndDateAndType(Long teacherId, java.time.LocalDate date, String type) {
        return jdbcClient.sql("""
                SELECT * FROM schedule
                WHERE teacher_id = ?
                  AND DATE(scheduled_at) = ?
                  AND schedule_type = ?
                ORDER BY scheduled_at
                """)
                .param(teacherId)
                .param(date)
                .param(type)
                .query(ROW_MAPPER)
                .list();
    }

    /** 교사의 기간별 일정 (시작일 ~ 종료일 포함) */
    public List<Schedule> findByTeacherAndDateRange(Long teacherId, java.time.LocalDate from, java.time.LocalDate to) {
        return jdbcClient.sql("""
                SELECT * FROM schedule
                WHERE teacher_id = ?
                  AND DATE(scheduled_at) BETWEEN ? AND ?
                ORDER BY scheduled_at
                """)
                .param(teacherId)
                .param(from)
                .param(to)
                .query(ROW_MAPPER)
                .list();
    }

    /** 일정 수정 (schedule_id 유지) */
    public int update(Schedule s) {
        return jdbcClient.sql("""
                UPDATE schedule
                   SET student_id    = ?,
                       schedule_type = ?,
                       title         = ?,
                       topic         = ?,
                       scheduled_at  = ?,
                       end_at        = ?,
                       location      = ?,
                       memo          = ?,
                       student_name  = ?,
                       parent_name   = ?
                 WHERE schedule_id = ?
                   AND teacher_id  = ?
                """)
                .param(s.getStudentId())
                .param(s.getScheduleType())
                .param(s.getTitle())
                .param(s.getTopic())
                .param(Timestamp.valueOf(s.getScheduledAt()))
                .param(s.getEndAt() != null ? Timestamp.valueOf(s.getEndAt()) : null)
                .param(s.getLocation())
                .param(s.getMemo())
                .param(s.getStudentName())
                .param(s.getParentName())
                .param(s.getScheduleId())
                .param(s.getTeacherId())
                .update();
    }

    /** 기존 schedule 유형 조회 (수정 전 유형 비교용) */
    public java.util.Optional<Schedule> findById(Long scheduleId) {
        return jdbcClient.sql("SELECT * FROM schedule WHERE schedule_id = ?")
                .param(scheduleId)
                .query(ROW_MAPPER)
                .optional();
    }

    public int deleteById(Long scheduleId, Long teacherId) {
        return jdbcClient.sql("DELETE FROM schedule WHERE schedule_id = ? AND teacher_id = ?")
                .param(scheduleId)
                .param(teacherId)
                .update();
    }
}
