package com.example.myapp.schedule.service;

import com.example.myapp.counseling.entity.CounselingNote;
import com.example.myapp.counseling.mapper.CounselingNoteMapper;
import com.example.myapp.schedule.dto.ScheduleCreateRequest;
import com.example.myapp.schedule.dto.ScheduleResponse;
import com.example.myapp.schedule.entity.Schedule;
import com.example.myapp.schedule.mapper.ScheduleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ScheduleService {

    private final ScheduleMapper scheduleMapper;
    private final CounselingNoteMapper counselingNoteMapper;

    public ScheduleService(ScheduleMapper scheduleMapper, CounselingNoteMapper counselingNoteMapper) {
        this.scheduleMapper = scheduleMapper;
        this.counselingNoteMapper = counselingNoteMapper;
    }

    @Transactional
    public ScheduleResponse create(Long teacherId, ScheduleCreateRequest req) {
        Schedule s = Schedule.builder()
                .teacherId(teacherId)
                .studentId(req.studentId())
                .scheduleType(req.scheduleType())
                .title(req.title())
                .topic(req.topic())
                .scheduledAt(req.scheduledAt())
                .endAt(req.endAt())
                .location(req.location())
                .memo(req.memo())
                .studentName(req.studentName())
                .parentName(req.parentName())
                .status("SCHEDULED")
                .build();
        Long id = scheduleMapper.insert(s);
        s.setScheduleId(id);

        // 상담 유형이면 counseling_note 빈 레코드 자동 생성
        if ("상담".equals(req.scheduleType())) {
            CounselingNote note = CounselingNote.builder()
                    .scheduleId(id)
                    .rawText(req.topic())       // 상담 주제를 초기 텍스트로
                    .summaryText(null)
                    .followUpText(null)
                    .build();
            counselingNoteMapper.insert(note);
        }

        return ScheduleResponse.from(s);
    }

    @Transactional
    public ScheduleResponse update(Long scheduleId, Long teacherId, ScheduleCreateRequest req) {
        Schedule existing = scheduleMapper.findById(scheduleId)
                .filter(s -> s.getTeacherId().equals(teacherId))
                .orElseThrow(() -> new RuntimeException("일정을 찾을 수 없습니다."));

        String oldType = existing.getScheduleType();
        String newType = req.scheduleType();

        Schedule updated = Schedule.builder()
                .scheduleId(scheduleId)
                .teacherId(teacherId)
                .studentId(req.studentId())
                .scheduleType(newType)
                .title(req.title())
                .topic(req.topic())
                .scheduledAt(req.scheduledAt())
                .endAt(req.endAt())
                .location(req.location())
                .memo(req.memo())
                .studentName(req.studentName())
                .parentName(req.parentName())
                .status(existing.getStatus())
                .build();

        scheduleMapper.update(updated);

        // counseling_note 처리
        boolean wasConsuling  = "상담".equals(oldType);
        boolean isNowCounseling = "상담".equals(newType);

        if (wasConsuling && !isNowCounseling) {
            // 상담 → 다른 유형: counseling_note 삭제
            List<CounselingNote> notes = counselingNoteMapper.findByScheduleId(scheduleId);
            notes.forEach(n -> counselingNoteMapper.deleteById(n.getCounselingNoteId()));
        } else if (!wasConsuling && isNowCounseling) {
            // 다른 유형 → 상담: counseling_note 신규 생성
            counselingNoteMapper.insert(CounselingNote.builder()
                    .scheduleId(scheduleId)
                    .rawText(req.topic())
                    .build());
        }
        // wasConsuling && isNowCounseling: 기존 counseling_note 유지 (데이터 보존)

        return ScheduleResponse.from(scheduleMapper.findById(scheduleId).orElseThrow());
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> list(Long teacherId, String type) {
        List<Schedule> rows = (type == null || type.isBlank())
                ? scheduleMapper.findByTeacher(teacherId)
                : scheduleMapper.findByTeacherAndType(teacherId, type);
        return rows.stream().map(ScheduleResponse::from).toList();
    }

    @Transactional
    public void delete(Long scheduleId, Long teacherId) {
        scheduleMapper.deleteById(scheduleId, teacherId);
    }
}
