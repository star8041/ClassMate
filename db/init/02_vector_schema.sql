-- ============================================
-- VectorDB 스키마 참고 문서
-- 실제 테이블은 Spring AI PgVectorStore가
-- initialize-schema=true 옵션으로 애플리케이션 기동 시 자동 생성하도록 할수 있다.
-- ============================================

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS vector_store (
                                            id        uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    content   text,
    metadata  json,
    embedding vector(1536)
    );

CREATE INDEX ON vector_store USING HNSW (embedding vector_cosine_ops);