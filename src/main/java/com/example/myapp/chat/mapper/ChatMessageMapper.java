package com.example.myapp.chat.mapper;

import com.example.myapp.chat.entity.ChatMessage;
import com.example.myapp.chat.entity.ChatRole;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
public class ChatMessageMapper {

    private final JdbcClient jdbcClient;

    public ChatMessageMapper(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    private static final RowMapper<ChatMessage> ROW_MAPPER = (rs, rowNum) -> ChatMessage.builder()
            .chatMessageId(rs.getLong("chat_message_id"))
            .chatSessionId(rs.getLong("chat_session_id"))
            .role(ChatRole.valueOf(rs.getString("role")))
            .messageText(rs.getString("message_text"))
            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
            .build();

    public Long insert(ChatMessage message) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql("""
                INSERT INTO chat_message (chat_session_id, role, message_text)
                VALUES (?, ?, ?)
                """)
                .param(message.getChatSessionId())
                .param(message.getRole().name())
                .param(message.getMessageText())
                .update(keyHolder, "chat_message_id");
        return keyHolder.getKey().longValue();
    }

    /**
     * 최근 N개 메시지를 시간 오름차순으로 반환.
     * (DESC로 가져온 뒤 reverse)
     */
    public List<ChatMessage> findRecentMessages(Long chatSessionId, int limit) {
        List<ChatMessage> messages = jdbcClient.sql("""
                SELECT * FROM chat_message
                WHERE chat_session_id = ?
                ORDER BY created_at DESC
                LIMIT ?
                """)
                .param(chatSessionId)
                .param(limit)
                .query(ROW_MAPPER)
                .list();
        Collections.reverse(messages);
        return messages;
    }

    /** 세션의 모든 메시지를 시간 오름차순으로 반환. */
    public List<ChatMessage> findAllBySessionId(Long chatSessionId) {
        return jdbcClient.sql("""
                SELECT * FROM chat_message
                WHERE chat_session_id = ?
                ORDER BY created_at ASC
                """)
                .param(chatSessionId)
                .query(ROW_MAPPER)
                .list();
    }
}
