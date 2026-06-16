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
}
