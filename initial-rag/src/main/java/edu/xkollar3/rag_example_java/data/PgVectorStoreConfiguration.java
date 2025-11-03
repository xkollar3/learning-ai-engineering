package edu.xkollar3.rag_example_java.data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "langchain-store")
public class PgVectorStoreConfiguration {

  private String hostname;
  private Integer port;
  private String username;
  private String password;
  private String database;
  private String embeddingTableName;
  private Integer embeddingDimensions;
}
