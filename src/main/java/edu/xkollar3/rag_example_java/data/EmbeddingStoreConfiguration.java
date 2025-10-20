package edu.xkollar3.rag_example_java.data;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class EmbeddingStoreConfiguration {

  @Bean
  public EmbeddingStore<TextSegment> embeddingStore(PgVectorStoreConfiguration configuration) {
    return PgVectorEmbeddingStore.builder()
        .host(configuration.getHostname())
        .port(configuration.getPort())
        .user(configuration.getUsername())
        .password(configuration.getPassword())
        .dimension(configuration.getEmbeddingDimensions())
        .table(configuration.getEmbeddingTableName())
        .database(configuration.getDatabase())
        .build();
  }
}
