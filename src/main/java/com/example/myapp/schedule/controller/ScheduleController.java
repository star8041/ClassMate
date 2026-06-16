package com.example.myapp.schedule.controller;

import com.example.myapp.schedule.dto.ScheduleCreateRequest;
import com.example.myapp.schedule.dto.ScheduleResponse;
import com.example.myapp.schedule.dto.ScheduleUpdateRequest;
import com.example.myapp.schedule.service.ScheduleService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 일정 관리 REST API. (PDF 명세서 기준)
 *
 * <pre>
 * POST   /api/v1/schedules                일정 등록
 * GET    /api/v1/schedules                일정 목록 조회
 * GET    /api/v1/schedules/{scheduleId}   일정 상세 조회
 * PATCH  /api/v1/schedules/{scheduleId}   일정 수정
 * DELETE /api/v1/schedules/{scheduleId}   일정 삭제
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    /** 일정 등록 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ScheduleResponse create(@RequestBody ScheduleCreateRequest request) {
        return scheduleService.create(request);
    }

    /** 일정 목록 조회 (teacherId 미지정 시 전체) */
    @GetMapping
    public List<ScheduleResponse> list(
            @RequestParam(value = "teacherId", required = false) Long teacherId) {
        return scheduleService.list(teacherId);
    }

    /** 일정 상세 조회 */
    @GetMapping("/{scheduleId}")
    public ScheduleResponse detail(@PathVariable("scheduleId") Long scheduleId) {
        return scheduleService.get(scheduleId);
    }

    /** 일정 수정 */
    @PatchMapping("/{scheduleId}")
    public ScheduleResponse update(@PathVariable("scheduleId") Long scheduleId,
                                   @RequestBody ScheduleUpdateRequest request) {
        return scheduleService.update(scheduleId, request);
    }

    /** 일정 삭제 */
    @DeleteMapping("/{scheduleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("scheduleId") Long scheduleId) {
        scheduleService.delete(scheduleId);
    }
}
