package edu.xkollar3.rag_example_java.data;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModelName;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import lombok.extern.slf4j.Slf4j;

// TODO: move
@Configuration
@Slf4j
public class EmbeddingConfig {
  @Bean
  public EmbeddingStore<TextSegment> embeddingStore(PgVectorStoreConfiguration configuration,
      EmbeddingModel openAiEmbeddingModel) {
    return PgVectorEmbeddingStore.builder()
        .host(configuration.getHostname())
        .port(configuration.getPort())
        .user(configuration.getUsername())
        .password(configuration.getPassword())
        .dimension(openAiEmbeddingModel.dimension())
        .table(configuration.getEmbeddingTableName())
        .database(configuration.getDatabase())
        .createTable(true)
        .build();
  }

  @Bean
  public EmbeddingModel embeddingModel(@Value("${openai-api-key}") String apiKey) {
    return OpenAiEmbeddingModel.builder().apiKey(apiKey).modelName(OpenAiEmbeddingModelName.TEXT_EMBEDDING_3_SMALL)
        .build();
  }

  @Bean
  public ChatModel chatModel(@Value("${openai-api-key}") String apiKey) {
    return OpenAiChatModel.builder().apiKey(apiKey).modelName(OpenAiChatModelName.GPT_3_5_TURBO).build();
  }
}
