package edu.xkollar3.contextual_retrieval_repository.configuration;

import jakarta.annotation.PostConstruct;

import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Configuration
@Slf4j
public class DatabaseInitialization {

  private final DataSource dataSource;

  @Autowired
  public DatabaseInitialization(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @PostConstruct
  public void initializeDatabase() {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      log.info("Initializing database schema for TF-IDF full text search...");

      statement.execute(
          "CREATE TABLE IF NOT EXISTS documents ("
              + "id UUID PRIMARY KEY, "
              + "name VARCHAR(255) NOT NULL"
              + ")");
      log.info("Documents table created successfully");

      statement.execute(
          "CREATE TABLE IF NOT EXISTS document_chunks ("
              + "id UUID PRIMARY KEY, "
              + "document_id UUID NOT NULL, "
              + "content TEXT NOT NULL, "
              + "contextualized_text TEXT NOT NULL, "
              + "search_vector tsvector, "
              + "chunk_index INTEGER NOT NULL, "
              + "embedding_id UUID, "
              + "FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE"
              + ")");
      log.info("DocumentChunks table created successfully");

      statement.execute(
          "CREATE INDEX IF NOT EXISTS idx_document_chunks_search_vector ON document_chunks "
              + "USING GIN(search_vector)");
      log.info("GIN index created successfully on document_chunks search_vector");

      statement.execute(
          "CREATE OR REPLACE FUNCTION update_document_chunk_search_vector() RETURNS trigger AS $$ "
              + "BEGIN "
              + "  new.search_vector := setweight(to_tsvector(coalesce((SELECT name FROM documents WHERE id = new.document_id), '')), 'A') || "
              + "      setweight(to_tsvector(coalesce(new.contextualized_text, '')), 'B'); "
              + "  return new; "
              + "END "
              + "$$ LANGUAGE plpgsql;");
      log.info("Trigger function update_document_chunk_search_vector created successfully");

      statement.execute(
          "DROP TRIGGER IF EXISTS trg_update_document_chunk_search_vector ON document_chunks");
      log.info("Dropped existing trigger if it exists");

      statement.execute(
          "CREATE TRIGGER trg_update_document_chunk_search_vector "
              + "BEFORE INSERT OR UPDATE ON document_chunks "
              + "FOR EACH ROW EXECUTE FUNCTION update_document_chunk_search_vector()");
      log.info("Trigger trg_update_document_chunk_search_vector created successfully");

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
}
