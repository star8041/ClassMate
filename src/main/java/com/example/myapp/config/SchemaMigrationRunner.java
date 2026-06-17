package com.example.myapp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 앱 시작 시 RDB(primary)에 누락된 스키마 변경을 멱등(idempotent)하게 자동 반영한다.
 * <p>
 * 팀원이 기존 DB에서 수동 마이그레이션(ALTER TABLE ...) 없이 pull 후 앱 실행만 하면
 * 필요한 컬럼이 자동으로 보강되도록 하기 위한 장치다. 모든 구문은 {@code IF NOT EXISTS}
 * 등 멱등하게 작성해 매 기동마다 안전하게 재실행된다.
 * <p>
 * NOTE: 본격적인 버전 관리가 필요하면 Flyway/Liquibase 도입을 권장한다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)   // 다른 초기화기(시더 등)보다 먼저 컬럼을 보장
public class SchemaMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaMigrationRunner.class);

    /** 매 기동 시 보장할 멱등 DDL 목록 */
    private static final String[] MIGRATIONS = {
            // 강의자료 분류(수업/참고) 컬럼
            "ALTER TABLE material ADD COLUMN IF NOT EXISTS category VARCHAR(20) NOT NULL DEFAULT 'LESSON'",
            // 일정: 폼 입력값(장소/메모/종료시각) + 상담 추가필드(학생/학부모) 보강
            "ALTER TABLE schedule ADD COLUMN IF NOT EXISTS end_at TIMESTAMP",
            "ALTER TABLE schedule ADD COLUMN IF NOT EXISTS location VARCHAR(200)",
            "ALTER TABLE schedule ADD COLUMN IF NOT EXISTS memo TEXT",
            "ALTER TABLE schedule ADD COLUMN IF NOT EXISTS student_name VARCHAR(100)",
            "ALTER TABLE schedule ADD COLUMN IF NOT EXISTS parent_name VARCHAR(100)"
    };

    private final JdbcTemplate jdbcTemplate;

    public SchemaMigrationRunner(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (String ddl : MIGRATIONS) {
            try {
                jdbcTemplate.execute(ddl);
                log.info("스키마 자동 마이그레이션 적용/확인 완료: {}", ddl);
            } catch (Exception e) {
                // 실패해도 앱 기동은 막지 않는다(로그만).
                log.warn("스키마 자동 마이그레이션 실패: {} ({})", ddl, e.getMessage());
            }
        }
    }
}
