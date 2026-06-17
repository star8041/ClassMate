package com.example.myapp.counseling.controller;

import com.example.myapp.counseling.dto.TranscriptionResponse;
import com.example.myapp.counseling.service.CounselingRecordingService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 상담 녹음 업로드 → 텍스트 변환 + 자동 요약 API.
 *
 * <pre>
 * POST /api/v1/counseling/transcribe   (multipart: file)  → { transcript, summary, charCount }
 * </pre>
 * 저장은 기존 {@code POST /api/v1/counseling-notes} 로 한다.
 */
@RestController
@RequestMapping("/api/v1/counseling")
public class CounselingRecordingController {

    private final CounselingRecordingService recordingService;

    public CounselingRecordingController(CounselingRecordingService recordingService) {
        this.recordingService = recordingService;
    }

    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TranscriptionResponse transcribe(@RequestParam("file") MultipartFile file) {
        return recordingService.transcribeAndSummarize(file);
    }
}
