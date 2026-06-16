package com.example.myapp.report.controller;

import com.example.myapp.common.ApiResponse;
import com.example.myapp.report.dto.ReportGenerateRequest;
import com.example.myapp.report.dto.ReportResponse;
import com.example.myapp.report.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 학생 지도 리포트 REST API.
 *
 * <pre>
 * POST   /api/v1/reports/students/{studentId}/generate  — 리포트 생성
 * GET    /api/v1/reports/students/{studentId}           — 리포트 목록 조회
 * GET    /api/v1/reports/{reportId}                     — 리포트 상세 조회
 * DELETE /api/v1/reports/{reportId}                     — 리포트 삭제
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /** 리포트 생성: 퀴즈 결과 + 상담 기록 → AI 분석 → DB 저장 */
    @PostMapping("/students/{studentId}/generate")
    public ResponseEntity<ApiResponse<ReportResponse>> generate(
            @PathVariable Long studentId,
            @Valid @RequestBody ReportGenerateRequest request) {

        return ResponseEntity.ok(ApiResponse.created(
                reportService.generateReport(studentId, request)));
    }

    /** 학생의 리포트 목록 조회 (최신순) */
    @GetMapping("/students/{studentId}")
    public ResponseEntity<ApiResponse<List<ReportResponse>>> getReports(
            @PathVariable Long studentId) {

        return ResponseEntity.ok(ApiResponse.success(
                reportService.getReports(studentId)));
    }

    /** 리포트 상세 조회 */
    @GetMapping("/{reportId}")
    public ResponseEntity<ApiResponse<ReportResponse>> getReport(
            @PathVariable Long reportId) {

        return ResponseEntity.ok(ApiResponse.success(
                reportService.getReport(reportId)));
    }

    /** 리포트 삭제 */
    @DeleteMapping("/{reportId}")
    public ResponseEntity<ApiResponse<Void>> deleteReport(
            @PathVariable Long reportId) {

        reportService.deleteReport(reportId);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
