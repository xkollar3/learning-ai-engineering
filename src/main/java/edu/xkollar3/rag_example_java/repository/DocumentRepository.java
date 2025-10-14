package edu.xkollar3.rag_example_java.repository;

import edu.xkollar3.rag_example_java.entity.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentEntity, UUID> {

  @Modifying
  @Transactional
  @Query(value = "DELETE FROM vector_store WHERE metadata->>'document_name' = :documentName", nativeQuery = true)
  void deleteByDocumentName(@Param("documentName") String documentName);
}
