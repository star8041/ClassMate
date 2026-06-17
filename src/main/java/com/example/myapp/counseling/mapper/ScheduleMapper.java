package com.example.myapp.counseling.mapper;

import com.example.myapp.counseling.entity.Schedule;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ScheduleMapper {

    private final JdbcClient jdbcClient;

    public ScheduleMapper(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Long insert(Schedule schedule) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql("""
                INSERT INTO schedule
                    (teacher_id, student_id, schedule_type, title, topic, scheduled_at, status)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """)
                .param(schedule.getTeacherId())
                .param(schedule.getStudentId())
                .param(schedule.getScheduleType())
                .param(schedule.getTitle())
                .param(schedule.getTopic())
                .param(schedule.getScheduledAt())
                .param(schedule.getStatus())
                .update(keyHolder, "schedule_id");
        return keyHolder.getKey().longValue();
    }

    public Optional<Schedule> findById(Long scheduleId) {
        return jdbcClient.sql("SELECT * FROM schedule WHERE schedule_id = ?")
                .param(scheduleId)
                .query((rs, rowNum) -> Schedule.builder()
                        .scheduleId(rs.getLong("schedule_id"))
                        .teacherId(rs.getLong("teacher_id"))
                        .studentId(rs.getLong("student_id"))
                        .scheduleType(rs.getString("schedule_type"))
                        .title(rs.getString("title"))
                        .topic(rs.getString("topic"))
                        .scheduledAt(rs.getTimestamp("scheduled_at").toLocalDateTime())
                        .status(rs.getString("status"))
                        .build())
                .optional();
    }

    public List<Schedule> findByTeacherId(Long teacherId) {
        return jdbcClient.sql("SELECT * FROM schedule WHERE teacher_id = ? ORDER BY scheduled_at")
                .param(teacherId)
                .query((rs, rowNum) -> Schedule.builder()
                        .scheduleId(rs.getLong("schedule_id"))
                        .teacherId(rs.getLong("teacher_id"))
                        .studentId(rs.getLong("student_id"))
                        .scheduleType(rs.getString("schedule_type"))
                        .title(rs.getString("title"))
                        .topic(rs.getString("topic"))
                        .scheduledAt(rs.getTimestamp("scheduled_at").toLocalDateTime())
                        .status(rs.getString("status"))
                        .build())
                .list();
    }
}
