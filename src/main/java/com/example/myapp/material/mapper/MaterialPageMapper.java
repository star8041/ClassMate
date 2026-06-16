package com.example.myapp.material.mapper;

import com.example.myapp.material.entity.MaterialPage;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * material_page 테이블 접근 매퍼.
 * AI Agent의 PAGE_SEARCH 의도 처리 시 SupervisorAgent가 사용한다.
 */
@Repository
public class MaterialPageMapper {

    private final JdbcClient jdbcClient;

    public MaterialPageMapper(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    private static final RowMapper<MaterialPage> ROW_MAPPER = (rs, rowNum) -> MaterialPage.builder()
            .materialPageId(rs.getLong("material_page_id"))
            .materialId(rs.getLong("material_id"))
            .pageNumber(rs.getInt("page_number"))
            .pageText(rs.getString("page_text"))
            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
            .build();

    /** 페이지 1건 저장 */
    public void insert(MaterialPage page) {
        jdbcClient.sql("""
                        INSERT INTO material_page (material_id, page_number, page_text)
                        VALUES (?, ?, ?)
                        """)
                .param(page.getMaterialId())
                .param(page.getPageNumber())
                .param(page.getPageText())
                .update();
    }

    /** 특정 자료의 페이지 목록 (페이지 번호순) */
    public List<MaterialPage> findByMaterialId(Long materialId) {
        return jdbcClient.sql("SELECT * FROM material_page WHERE material_id = ? ORDER BY page_number")
                .param(materialId)
                .query(ROW_MAPPER)
                .list();
    }

    /** 페이지 번호 범위로 조회 — PAGE_SEARCH 의도 처리 시 사용 */
    public List<MaterialPage> findByPageRange(Long materialId, int startPage, int endPage) {
        return jdbcClient.sql("""
                SELECT * FROM material_page
                WHERE material_id = ? AND page_number BETWEEN ? AND ?
                ORDER BY page_number ASC
                """)
                .param(materialId)
                .param(startPage)
                .param(endPage)
                .query(ROW_MAPPER)
                .list();
    }

    /** 키워드 포함 페이지 검색 (LIKE) — PAGE_SEARCH 의도 처리 시 사용 */
    public List<MaterialPage> searchByKeyword(Long materialId, String keyword) {
        return jdbcClient.sql("""
                SELECT * FROM material_page
                WHERE material_id = ? AND LOWER(page_text) LIKE LOWER(?)
                ORDER BY page_number ASC
                """)
                .param(materialId)
                .param("%" + keyword + "%")
                .query(ROW_MAPPER)
                .list();
    }
}
