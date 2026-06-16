package com.example.myapp.report.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ReportGenerateRequest(
        @NotNull(message = "분석 시작일을 입력해 주세요.")
        LocalDate periodStart,

        @NotNull(message = "분석 종료일을 입력해 주세요.")
        LocalDate periodEnd
) {}
