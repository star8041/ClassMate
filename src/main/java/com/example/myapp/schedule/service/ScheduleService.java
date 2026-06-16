package com.example.myapp.schedule.service;

import com.example.myapp.schedule.dto.ScheduleCreateRequest;
import com.example.myapp.schedule.dto.ScheduleResponse;
import com.example.myapp.schedule.dto.ScheduleUpdateRequest;
import com.example.myapp.schedule.entity.Schedule;
import com.example.myapp.schedule.mapper.ScheduleMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

/**
 * 일정 관리 비즈니스 로직 (등록/조회/수정/삭제).
 */
@Service
public class ScheduleService {

    private static final Set<String> SCHEDULE_TYPES = Set.of("COUNSELING", "CLASS", "ETC");
    private static final String DEFAULT_STATUS = "SCHEDULED";

    private final ScheduleMapper scheduleMapper;

    public ScheduleService(ScheduleMapper scheduleMapper) {
        this.scheduleMapper = scheduleMapper;
    }

    /** 일정 등록 */
    @Transactional
    public ScheduleResponse create(ScheduleCreateRequest request) {
        if (request.teacherId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "teacherId 는 필수입니다.");
        }
        if (!StringUtils.hasText(request.title())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "일정 제목은 필수입니다.");
        }
        if (request.scheduledAt() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "일정 시각(scheduledAt)은 필수입니다.");
        }
        validateType(request.scheduleType());

        Schedule schedule = Schedule.builder()
                .teacherId(request.teacherId())
                .studentId(request.studentId())
                .scheduleType(request.scheduleType())
                .title(request.title())
                .topic(request.topic())
                .scheduledAt(request.scheduledAt())
                .status(StringUtils.hasText(request.status()) ? request.status() : DEFAULT_STATUS)
                .calendarEventId(request.calendarEventId())
                .build();

        try {
            Long id = scheduleMapper.insert(schedule);
            return ScheduleResponse.from(getEntityOrThrow(id));
        } catch (DataIntegrityViolationException e) {
            // teacher_id / student_id FK 위반 등
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "존재하지 않는 교사 또는 학생입니다.", e);
        }
    }

    /** 일정 목록 조회 (teacherId 가 null 이면 전체) */
    @Transactional(readOnly = true)
    public List<ScheduleResponse> list(Long teacherId) {
        return scheduleMapper.findAll(teacherId).stream()
                .map(ScheduleResponse::from)
                .toList();
    }

    /** 일정 상세 조회 */
    @Transactional(readOnly = true)
    public ScheduleResponse get(Long scheduleId) {
        return ScheduleResponse.from(getEntityOrThrow(scheduleId));
    }

    /** 일정 부분 수정 (PATCH): 전달된 필드만 변경 */
    @Transactional
    public ScheduleResponse update(Long scheduleId, ScheduleUpdateRequest request) {
        Schedule schedule = getEntityOrThrow(scheduleId);

        if (request.studentId() != null) {
            schedule.setStudentId(request.studentId());
        }
        if (request.scheduleType() != null) {
            validateType(request.scheduleType());
            schedule.setScheduleType(request.scheduleType());
        }
        if (request.title() != null) {
            if (!StringUtils.hasText(request.title())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "일정 제목은 비어 있을 수 없습니다.");
            }
            schedule.setTitle(request.title());
        }
        if (request.topic() != null) {
            schedule.setTopic(request.topic());
        }
        if (request.scheduledAt() != null) {
            schedule.setScheduledAt(request.scheduledAt());
        }
        if (request.status() != null) {
            schedule.setStatus(request.status());
        }
        if (request.calendarEventId() != null) {
            schedule.setCalendarEventId(request.calendarEventId());
        }

        try {
            scheduleMapper.update(schedule);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "존재하지 않는 학생입니다.", e);
        }
        return ScheduleResponse.from(getEntityOrThrow(scheduleId));
    }

    /** 일정 삭제 */
    @Transactional
    public void delete(Long scheduleId) {
        getEntityOrThrow(scheduleId);
        scheduleMapper.deleteById(scheduleId);
    }

    // ===== 내부 헬퍼 =====

    private Schedule getEntityOrThrow(Long scheduleId) {
        return scheduleMapper.findById(scheduleId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "일정을 찾을 수 없습니다. id=" + scheduleId));
    }

    private void validateType(String scheduleType) {
        if (!StringUtils.hasText(scheduleType) || !SCHEDULE_TYPES.contains(scheduleType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "scheduleType 은 COUNSELING / CLASS / ETC 중 하나여야 합니다.");
        }
    }
}
