package com.example.myapp.report.dto;

/**
 * Spring AI structured output 역직렬화 대상.
 * report-generator.st 프롬프트의 JSON 출력과 필드명이 일치해야 한다.
 */
public record ReportResult(
        String summaryText,
        String strengthText,
        String weaknessText,
        String commentText
) {}
