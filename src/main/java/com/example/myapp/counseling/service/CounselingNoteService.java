package com.example.myapp.counseling.service;

import com.example.myapp.counseling.dto.CounselingNoteCreateRequest;
import com.example.myapp.counseling.dto.CounselingNoteResponse;
import com.example.myapp.counseling.entity.CounselingNote;
import com.example.myapp.counseling.mapper.CounselingNoteMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * 상담 기록 비즈니스 로직 (등록 / 일정별 조회).
 */
@Service
public class CounselingNoteService {

    private final CounselingNoteMapper counselingNoteMapper;

    public CounselingNoteService(CounselingNoteMapper counselingNoteMapper) {
        this.counselingNoteMapper = counselingNoteMapper;
    }

    /** 상담 기록 등록 */
    @Transactional
    public CounselingNoteResponse create(CounselingNoteCreateRequest request) {
        if (request.scheduleId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "scheduleId 는 필수입니다.");
        }

        CounselingNote note = CounselingNote.builder()
                .scheduleId(request.scheduleId())
                .rawText(request.rawText())
                .summaryText(request.summaryText())
                .followUpText(request.followUpText())
                .build();

        try {
            Long id = counselingNoteMapper.insert(note);
            return CounselingNoteResponse.from(
                    counselingNoteMapper.findById(id).orElse(note));
        } catch (DataIntegrityViolationException e) {
            // schedule_id FK 위반
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "존재하지 않는 상담 일정입니다. scheduleId=" + request.scheduleId(), e);
        }
    }

    /** 특정 상담 일정의 기록 목록 조회 */
    @Transactional(readOnly = true)
    public List<CounselingNoteResponse> listBySchedule(Long scheduleId) {
        if (scheduleId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "scheduleId 는 필수입니다.");
        }
        return counselingNoteMapper.findByScheduleId(scheduleId).stream()
                .map(CounselingNoteResponse::from)
                .toList();
    }
}
