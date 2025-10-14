package edu.xkollar3.rag_example_java.api;

import edu.xkollar3.rag_example_java.service.DocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/documents")
@Slf4j
public class DocumentApi {

  private final DocumentService documentService;

  @Autowired
  public DocumentApi(DocumentService documentService) {
    this.documentService = documentService;
  }

  @PostMapping("/load")
  public ResponseEntity<String> loadDocument(
      @RequestParam("file") MultipartFile file,
      @RequestParam("documentName") String documentName) {
    try {
      documentService.loadDocumentUnderName(file, documentName);
      return ResponseEntity.ok("Document loaded successfully");
    } catch (IOException e) {
      log.error("Error loading document: {}", e.getMessage());
      return ResponseEntity.badRequest().body("Error loading document: " + e.getMessage());
    }
  }

  @DeleteMapping("/{documentName}")
  public ResponseEntity<String> deleteDocument(@PathVariable String documentName) {
    try {
      documentService.deleteByDocumentName(documentName);
      return ResponseEntity.ok("Document deleted successfully");
    } catch (Exception e) {
      log.error("Error deleting document: {}", e.getMessage());
      return ResponseEntity.badRequest().body("Error deleting document: " + e.getMessage());
    }
  }

  @PostMapping("/reindex")
  public ResponseEntity<String> reindexDocument(
      @RequestParam("file") MultipartFile file,
      @RequestParam("documentName") String documentName) {
    try {
      documentService.reindexDocument(file, documentName);
      return ResponseEntity.ok("Document reindexed successfully");
    } catch (IOException e) {
      log.error("Error reindexing document: {}", e.getMessage());
      return ResponseEntity.badRequest().body("Error reindexing document: " + e.getMessage());
    }
  }
}
