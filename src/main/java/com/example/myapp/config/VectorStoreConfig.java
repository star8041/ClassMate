package com.example.myapp.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Spring AI 벡터 스토어 설정.
 * <p>
 * 별도의 벡터 DB(5433)에 연결된 {@code vectorJdbcTemplate} 과 OpenAI 임베딩 모델을 사용해
 * 기존 {@code vector_store} 테이블(id/content/metadata/embedding vector(1536))에 직접 매핑한다.
 * 테이블은 DDL 로 이미 생성돼 있으므로 initializeSchema=false.
 * <p>
 * 이 빈을 직접 정의하면 spring-ai-starter-vector-store-pgvector 의 자동설정
 * (Primary 데이터소스를 쓰는 PgVectorStore)은 @ConditionalOnMissingBean 으로 비활성화된다.
 */
@Configuration
public class VectorStoreConfig {

    @Bean
    public VectorStore vectorStore(@Qualifier("vectorJdbcTemplate") JdbcTemplate vectorJdbcTemplate,
                                   EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(vectorJdbcTemplate, embeddingModel)
                .schemaName("public")
                .vectorTableName("vector_store")
                .dimensions(1536)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .initializeSchema(false)
                .build();
    }
}
