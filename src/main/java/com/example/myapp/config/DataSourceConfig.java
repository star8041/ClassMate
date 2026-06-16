package com.example.myapp.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 멀티 데이터소스 설정.
 * <ul>
 *     <li><b>RDB (Primary)</b>: 일반 업무 데이터 (5432, classmate) — JdbcClient/MyBatis/트랜잭션이 기본 사용</li>
 *     <li><b>Vector DB</b>: 임베딩 저장 (5433, classmate_vector) — Spring AI PgVectorStore 전용</li>
 * </ul>
 */
@Configuration
public class DataSourceConfig {

    // ===== RDB (Primary) =====

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties rdbDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    public DataSource rdbDataSource(@Qualifier("rdbDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean
    @Primary
    public JdbcTemplate jdbcTemplate(@Qualifier("rdbDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    // ===== Vector DB =====

    @Bean
    @ConfigurationProperties("app.vector.datasource")
    public DataSourceProperties vectorDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource vectorDataSource(@Qualifier("vectorDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean
    public JdbcTemplate vectorJdbcTemplate(@Qualifier("vectorDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
