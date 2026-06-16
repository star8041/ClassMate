package com.example.myapp.counseling.mapper;

import com.example.myapp.counseling.entity.CounselingNote;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * counseling_note 테이블 접근 매퍼 (JdbcClient).
 */
@Repository
public class CounselingNoteMapper {

    private final JdbcClient jdbcClient;

    public CounselingNoteMapper(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    private static final RowMapper<CounselingNote> ROW_MAPPER = (rs, rowNum) -> CounselingNote.builder()
            .counselingNoteId(rs.getLong("counseling_note_id"))
            .scheduleId(rs.getLong("schedule_id"))
            .rawText(rs.getString("raw_text"))
            .summaryText(rs.getString("summary_text"))
            .followUpText(rs.getString("follow_up_text"))
            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
            .build();

    /** 새 상담 기록을 저장하고 생성된 PK를 반환한다. */
    public Long insert(CounselingNote note) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql("""
                        INSERT INTO counseling_note (schedule_id, raw_text, summary_text, follow_up_text)
                        VALUES (?, ?, ?, ?)
                        """)
                .param(note.getScheduleId())
                .param(note.getRawText())
                .param(note.getSummaryText())
                .param(note.getFollowUpText())
                .update(keyHolder, "counseling_note_id");
        return keyHolder.getKey().longValue();
    }

    /** 단건 조회 */
    public Optional<CounselingNote> findById(Long counselingNoteId) {
        return jdbcClient.sql("SELECT * FROM counseling_note WHERE counseling_note_id = ?")
                .param(counselingNoteId)
                .query(ROW_MAPPER)
                .optional();
    }

    /** 특정 상담 일정의 기록 목록을 최신순으로 조회한다. */
    public List<CounselingNote> findByScheduleId(Long scheduleId) {
        return jdbcClient.sql("SELECT * FROM counseling_note WHERE schedule_id = ? ORDER BY created_at DESC")
                .param(scheduleId)
                .query(ROW_MAPPER)
                .list();
    }

    /**
     * 특정 학생의 기간 내 상담 기록을 조회한다.
     * schedule 테이블과 JOIN 하여 student_id 와 기간으로 필터링한다.
     */
    public List<CounselingNote> findByStudentIdAndPeriod(Long studentId,
                                                          LocalDate periodStart,
                                                          LocalDate periodEnd) {
        return jdbcClient.sql("""
                SELECT cn.*
                FROM counseling_note cn
                JOIN schedule s ON cn.schedule_id = s.schedule_id
                WHERE s.student_id = ?
                  AND s.scheduled_at >= ?
                  AND s.scheduled_at < ?
                ORDER BY s.scheduled_at
                """)
                .param(studentId)
                .param(periodStart.atStartOfDay())
                .param(periodEnd.plusDays(1).atStartOfDay())
                .query(ROW_MAPPER)
                .list();
    }
}
