package com.example.myapp.counseling.service;

import com.example.myapp.counseling.dto.TranscriptionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * 상담 녹음 파일 → 텍스트 변환(OpenAI Whisper) + 자동 요약(OpenAI Chat).
 * <p>
 * 실제 변환/요약에는 유효한 OpenAI API 키가 필요하다. (채팅 기능과 동일 키)
 */
@Service
public class CounselingRecordingService {

    private static final Logger log = LoggerFactory.getLogger(CounselingRecordingService.class);

    private final OpenAiAudioTranscriptionModel transcriptionModel;
    private final ChatClient chatClient;

    public CounselingRecordingService(OpenAiAudioTranscriptionModel transcriptionModel,
                                      ChatClient chatClient) {
        this.transcriptionModel = transcriptionModel;
        this.chatClient = chatClient;
    }

    public TranscriptionResponse transcribeAndSummarize(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "녹음 파일이 비어 있습니다.");
        }

        // 1) STT (Whisper)
        String transcript;
        try {
            transcript = transcriptionModel
                    .call(new AudioTranscriptionPrompt(file.getResource()))
                    .getResult()
                    .getOutput();
        } catch (Exception e) {
            log.warn("음성 변환 실패: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "음성 변환에 실패했습니다. (OpenAI 음성 인식 호출 오류 — API 키/네트워크 확인)");
        }
        if (transcript == null || transcript.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "녹음에서 텍스트를 추출하지 못했습니다.");
        }

        // 2) 자동 요약 (실패해도 변환 결과는 반환)
        String summary = "";
        try {
            summary = chatClient.prompt()
                    .user("""
                            아래는 학부모 상담 녹음을 받아쓴 내용입니다.
                            교사가 빠르게 확인할 수 있도록 한국어로 핵심만 4~6개의 불릿으로 요약해 주세요.
                            각 줄은 '- '로 시작하고, 불릿 외 다른 문장은 출력하지 마세요.

                            [상담 내용]
                            %s
                            """.formatted(transcript))
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("자동 요약 실패: {}", e.getMessage());
        }

        return new TranscriptionResponse(transcript, summary == null ? "" : summary.trim(),
                transcript.length());
    }
}
