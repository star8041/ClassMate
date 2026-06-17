package com.example.myapp.material.mapper;

import com.example.myapp.material.entity.Material;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * material 테이블 접근 매퍼.
 * <p>
 * 패키지 컨벤션(mapper)을 따르되, Spring Boot 4 / Java 25 환경에서
 * MyBatis 스타터의 호환 릴리스가 아직 없어 Spring {@link JdbcClient}로 구현했다.
 */
@Repository
public class MaterialMapper {

    private final JdbcClient jdbcClient;

    public MaterialMapper(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /** material 행 → Material 엔티티 매핑 (snake_case → camelCase) */
    private static final RowMapper<Material> ROW_MAPPER = (rs, rowNum) -> Material.builder()
            .materialId(rs.getLong("material_id"))
            .teacherId(rs.getLong("teacher_id"))
            .fileName(rs.getString("file_name"))
            .subject(rs.getString("subject"))
            .category(rs.getString("category"))
            .storagePath(rs.getString("storage_path"))
            .totalPages(rs.getInt("total_pages"))
            .uploadedAt(rs.getTimestamp("uploaded_at").toLocalDateTime())
            .build();

    /**
     * 새 PDF 자료를 저장하고 생성된 PK를 반환한다.
     */
    public Long insert(Material material) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql("""
                        INSERT INTO material (teacher_id, file_name, subject, category, storage_path, total_pages)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """)
                .param(material.getTeacherId())
                .param(material.getFileName())
                .param(material.getSubject())
                .param(material.getCategory())
                .param(material.getStoragePath())
                .param(material.getTotalPages())
                .update(keyHolder, "material_id");
        return keyHolder.getKey().longValue();
    }

    /**
     * 전체 또는 특정 교사의 PDF 자료 목록을 최신순으로 조회한다.
     *
     * @param teacherId null 이면 전체 조회
     */
    public List<Material> findAll(Long teacherId) {
        if (teacherId == null) {
            return jdbcClient.sql("SELECT * FROM material ORDER BY uploaded_at DESC")
                    .query(ROW_MAPPER)
                    .list();
        }
        return jdbcClient.sql("SELECT * FROM material WHERE teacher_id = ? ORDER BY uploaded_at DESC")
                .param(teacherId)
                .query(ROW_MAPPER)
                .list();
    }

    /** 단건 조회 */
    public Optional<Material> findById(Long materialId) {
        return jdbcClient.sql("SELECT * FROM material WHERE material_id = ?")
                .param(materialId)
                .query(ROW_MAPPER)
                .optional();
    }

    /** 교사의 교재를 파일명·과목명 키워드로 검색 (공백 무시, 대소문자 무시) */
    public List<Material> findByTeacherAndKeyword(Long teacherId, String keyword) {
        // 검색어에서 공백 제거 → 파일명의 공백도 제거 후 LIKE 비교
        String normalizedKeyword = "%" + keyword.replaceAll("\\s+", "") + "%";
        return jdbcClient.sql("""
                SELECT * FROM material
                WHERE teacher_id = ?
                  AND (
                    LOWER(REPLACE(file_name, ' ', '')) LIKE LOWER(?)
                    OR LOWER(COALESCE(subject, '')) LIKE LOWER(?)
                  )
                ORDER BY uploaded_at DESC
                """)
                .param(teacherId)
                .param(normalizedKeyword)
                .param("%" + keyword + "%")
                .query(ROW_MAPPER)
                .list();
    }

    /** 삭제 (삭제된 행 수 반환) */
    public int deleteById(Long materialId) {
        return jdbcClient.sql("DELETE FROM material WHERE material_id = ?")
                .param(materialId)
                .update();
    }
}
