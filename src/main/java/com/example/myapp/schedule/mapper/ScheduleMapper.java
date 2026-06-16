package com.example.myapp.schedule.mapper;

import com.example.myapp.schedule.entity.Schedule;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * schedule 테이블 접근 매퍼 (JdbcClient).
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
            .status(rs.getString("status"))
            .calendarEventId(rs.getString("calendar_event_id"))
            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
            .build();

    /** 새 일정을 저장하고 생성된 PK를 반환한다. */
    public Long insert(Schedule schedule) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql("""
                        INSERT INTO schedule
                            (teacher_id, student_id, schedule_type, title, topic, scheduled_at, status, calendar_event_id)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """)
                .param(schedule.getTeacherId())
                .param(schedule.getStudentId())
                .param(schedule.getScheduleType())
                .param(schedule.getTitle())
                .param(schedule.getTopic())
                .param(schedule.getScheduledAt())
                .param(schedule.getStatus())
                .param(schedule.getCalendarEventId())
                .update(keyHolder, "schedule_id");
        return keyHolder.getKey().longValue();
    }

    /**
     * 전체 또는 특정 교사의 일정 목록을 예정 시각순으로 조회한다.
     *
     * @param teacherId null 이면 전체 조회
     */
    public List<Schedule> findAll(Long teacherId) {
        if (teacherId == null) {
            return jdbcClient.sql("SELECT * FROM schedule ORDER BY scheduled_at")
                    .query(ROW_MAPPER)
                    .list();
        }
        return jdbcClient.sql("SELECT * FROM schedule WHERE teacher_id = ? ORDER BY scheduled_at")
                .param(teacherId)
                .query(ROW_MAPPER)
                .list();
    }

    /** 단건 조회 */
    public Optional<Schedule> findById(Long scheduleId) {
        return jdbcClient.sql("SELECT * FROM schedule WHERE schedule_id = ?")
                .param(scheduleId)
                .query(ROW_MAPPER)
                .optional();
    }

    /** 일정 수정 (담당 교사 제외 변경 가능 컬럼 갱신, 수정된 행 수 반환) */
    public int update(Schedule schedule) {
        return jdbcClient.sql("""
                        UPDATE schedule
                           SET student_id = ?, schedule_type = ?, title = ?, topic = ?,
                               scheduled_at = ?, status = ?, calendar_event_id = ?
                         WHERE schedule_id = ?
                        """)
                .param(schedule.getStudentId())
                .param(schedule.getScheduleType())
                .param(schedule.getTitle())
                .param(schedule.getTopic())
                .param(schedule.getScheduledAt())
                .param(schedule.getStatus())
                .param(schedule.getCalendarEventId())
                .param(schedule.getScheduleId())
                .update();
    }

    /** 삭제 (삭제된 행 수 반환) */
    public int deleteById(Long scheduleId) {
        return jdbcClient.sql("DELETE FROM schedule WHERE schedule_id = ?")
                .param(scheduleId)
                .update();
    }
}
