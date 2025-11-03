package edu.xkollar3.contextual_retrieval_repository.repository;

import edu.xkollar3.contextual_retrieval_repository.model.DocumentChunkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunkEntity, UUID> {
  List<DocumentChunkEntity> findByDocumentId(UUID documentId);

  @Query(value = """
      SELECT dc.*
      FROM document_chunks dc
      ORDER BY ts_rank(dc.search_vector, websearch_to_tsquery(:query)) DESC
      LIMIT :k
      """, nativeQuery = true)
  List<DocumentChunkEntity> searchByQuery(@Param("query") String query, @Param("k") int k);
}
