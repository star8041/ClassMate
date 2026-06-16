package com.example.myapp.chat.controller;

import com.example.myapp.chat.dto.ChatMessageRequest;
import com.example.myapp.chat.dto.ChatSessionCreateRequest;
import com.example.myapp.chat.dto.ChatSessionResponse;
import com.example.myapp.chat.entity.ChatRole;
import com.example.myapp.chat.service.ChatSessionService;
import com.example.myapp.chat.service.TeacherChatService;
import com.example.myapp.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teacher-chat")
@RequiredArgsConstructor
public class TeacherChatController {

    private final ChatSessionService chatSessionService;
    private final TeacherChatService teacherChatService;

    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<ChatSessionResponse>> createSession(
            @AuthenticationPrincipal Long teacherId,
            @RequestBody ChatSessionCreateRequest request) {

        return ResponseEntity.ok(ApiResponse.created(
                chatSessionService.createSession(teacherId, ChatRole.TEACHER, request)));
    }

    @PostMapping(value = "/sessions/{sessionId}/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> sendMessage(
            @AuthenticationPrincipal Long teacherId,
//            @PathVariable Long sessionId,
            @PathVariable("sessionId") Long sessionId,
            @Valid @RequestBody ChatMessageRequest request) {

        return teacherChatService.sendMessage(teacherId, sessionId, request.messageText());
    }

    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<List<ChatSessionResponse>>> getSessions(
            @AuthenticationPrincipal Long teacherId) {

        return ResponseEntity.ok(ApiResponse.success(
                chatSessionService.getSessions(teacherId, ChatRole.TEACHER)));
    }
}
