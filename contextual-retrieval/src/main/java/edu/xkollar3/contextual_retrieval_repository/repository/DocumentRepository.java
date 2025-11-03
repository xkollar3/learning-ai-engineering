package edu.xkollar3.contextual_retrieval_repository.repository;

import edu.xkollar3.contextual_retrieval_repository.model.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentEntity, UUID> {
}
