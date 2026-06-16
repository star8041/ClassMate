package com.example.myapp.chat.mapper;

import com.example.myapp.chat.entity.ChatRole;
import com.example.myapp.chat.entity.ChatSession;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ChatSessionMapper {

    private final JdbcClient jdbcClient;

    public ChatSessionMapper(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    private static final RowMapper<ChatSession> ROW_MAPPER = (rs, rowNum) -> ChatSession.builder()
            .chatSessionId(rs.getLong("chat_session_id"))
            .userId(rs.getLong("user_id"))
            .role(ChatRole.valueOf(rs.getString("role")))
            .materialId(rs.getObject("material_id", Long.class))
            .title(rs.getString("title"))
            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
            .build();

    public Long insert(ChatSession session) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql("""
                INSERT INTO chat_session (user_id, role, material_id, title)
                VALUES (?, ?, ?, ?)
                """)
                .param(session.getUserId())
                .param(session.getRole().name())
                .param(session.getMaterialId())
                .param(session.getTitle())
                .update(keyHolder, "chat_session_id");
        return keyHolder.getKey().longValue();
    }

    public Optional<ChatSession> findById(Long chatSessionId) {
        return jdbcClient.sql("SELECT * FROM chat_session WHERE chat_session_id = ?")
                .param(chatSessionId)
                .query(ROW_MAPPER)
                .optional();
    }

    public Optional<ChatSession> findByIdAndUserId(Long chatSessionId, Long userId) {
        return jdbcClient.sql("""
                SELECT * FROM chat_session
                WHERE chat_session_id = ? AND user_id = ?
                """)
                .param(chatSessionId)
                .param(userId)
                .query(ROW_MAPPER)
                .optional();
    }

    public List<ChatSession> findByUserIdAndRole(Long userId, ChatRole role) {
        return jdbcClient.sql("""
                SELECT * FROM chat_session
                WHERE user_id = ? AND role = ?
                ORDER BY created_at DESC
                """)
                .param(userId)
                .param(role.name())
                .query(ROW_MAPPER)
                .list();
    }
}
