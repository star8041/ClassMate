package com.example.myapp.counseling.controller;

import com.example.myapp.counseling.dto.CounselingNoteCreateRequest;
import com.example.myapp.counseling.dto.CounselingNoteResponse;
import com.example.myapp.counseling.service.CounselingNoteService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 상담 기록 REST API. (PDF 명세서 기준)
 *
 * <pre>
 * POST /api/v1/counseling-notes                      상담 기록(녹음→요약) 등록
 * GET  /api/v1/counseling-notes?scheduleId={id}      상담 일정별 기록 조회
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/counseling-notes")
public class CounselingNoteController {

    private final CounselingNoteService counselingNoteService;

    public CounselingNoteController(CounselingNoteService counselingNoteService) {
        this.counselingNoteService = counselingNoteService;
    }

    /** 상담 기록 등록 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CounselingNoteResponse create(@RequestBody CounselingNoteCreateRequest request) {
        return counselingNoteService.create(request);
    }

    /** 상담 일정별 기록 조회 */
    @GetMapping
    public List<CounselingNoteResponse> listBySchedule(
            @RequestParam("scheduleId") Long scheduleId) {
        return counselingNoteService.listBySchedule(scheduleId);
    }
}
