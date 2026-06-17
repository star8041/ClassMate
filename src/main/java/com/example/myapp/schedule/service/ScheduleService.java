package com.example.myapp.schedule.service;

import com.example.myapp.schedule.dto.ScheduleCreateRequest;
import com.example.myapp.schedule.dto.ScheduleResponse;
import com.example.myapp.schedule.entity.Schedule;
import com.example.myapp.schedule.mapper.ScheduleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ScheduleService {

    private final ScheduleMapper scheduleMapper;

    public ScheduleService(ScheduleMapper scheduleMapper) {
        this.scheduleMapper = scheduleMapper;
    }

    @Transactional
    public ScheduleResponse create(Long teacherId, ScheduleCreateRequest req) {
        Schedule s = Schedule.builder()
                .teacherId(teacherId)
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
        return ScheduleResponse.from(s);
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
