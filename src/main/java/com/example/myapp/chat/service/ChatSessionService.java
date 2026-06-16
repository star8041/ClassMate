package com.example.myapp.chat.service;

import com.example.myapp.chat.dto.ChatSessionCreateRequest;
import com.example.myapp.chat.dto.ChatSessionResponse;
import com.example.myapp.chat.entity.ChatRole;
import com.example.myapp.chat.entity.ChatSession;
import com.example.myapp.chat.mapper.ChatSessionMapper;
import com.example.myapp.common.exception.BusinessException;
import com.example.myapp.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private final ChatSessionMapper chatSessionMapper;

    @Transactional
    public ChatSessionResponse createSession(Long userId, ChatRole role, ChatSessionCreateRequest request) {
        ChatSession session = ChatSession.builder()
                .userId(userId)
                .role(role)
                .materialId(request.materialId())
                .title(request.title())
                .build();

        Long id = chatSessionMapper.insert(session);
        return ChatSessionResponse.from(chatSessionMapper.findById(id).orElseThrow());
    }

    public List<ChatSessionResponse> getSessions(Long userId, ChatRole role) {
        return chatSessionMapper.findByUserIdAndRole(userId, role).stream()
                .map(ChatSessionResponse::from)
                .toList();
    }

    public ChatSession getOwnedSession(Long chatSessionId, Long userId) {
        return chatSessionMapper.findByIdAndUserId(chatSessionId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_SESSION_NOT_FOUND));
    }

    @Transactional
    public void updateTitle(Long chatSessionId, Long userId, String title) {
        getOwnedSession(chatSessionId, userId); // 본인 세션 검증
        chatSessionMapper.updateTitle(chatSessionId, title);
    }

    @Transactional
    public void deleteSession(Long chatSessionId, Long userId) {
        getOwnedSession(chatSessionId, userId); // 본인 세션 검증
        chatSessionMapper.deleteById(chatSessionId);
    }
}
