package com.example.myapp.schedule.controller;

import com.example.myapp.schedule.dto.ScheduleCreateRequest;
import com.example.myapp.schedule.dto.ScheduleResponse;
import com.example.myapp.schedule.service.ScheduleService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 교사 일정 API.
 * <pre>
 * POST   /api/schedules           일정 등록
 * GET    /api/schedules           내 일정 목록 (?type=상담 등 유형 필터)
 * DELETE /api/schedules/{id}      일정 삭제
 * </pre>
 * 인증된 교사(JWT principal = teacherId) 기준으로 동작한다.
 */
@RestController
@RequestMapping("/api/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ScheduleResponse create(@AuthenticationPrincipal Long teacherId,
                                   @RequestBody ScheduleCreateRequest request) {
        return scheduleService.create(teacherId, request);
    }

    @GetMapping
    public List<ScheduleResponse> list(@AuthenticationPrincipal Long teacherId,
                                       @RequestParam(value = "type", required = false) String type) {
        return scheduleService.list(teacherId, type);
    }

    @PutMapping("/{id}")
    public ScheduleResponse update(@AuthenticationPrincipal Long teacherId,
                                   @PathVariable("id") Long id,
                                   @RequestBody ScheduleCreateRequest request) {
        return scheduleService.update(id, teacherId, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Long teacherId, @PathVariable("id") Long id) {
        scheduleService.delete(id, teacherId);
    }
}
