package edu.xkollar3.contextual_retrieval_repository.api;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import edu.xkollar3.contextual_retrieval_repository.service.DocumentService;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/documents")
@Slf4j
public class DocumentApi {

  private final DocumentService documentService;

  @Autowired
  public DocumentApi(DocumentService documentService) {
    this.documentService = documentService;
  }

  @PostMapping
  public ResponseEntity<String> uploadDocument(
      @RequestParam("file") MultipartFile file,
      @RequestParam("documentName") String documentName) throws IOException {
    documentService.uploadDocument(file, documentName);
    return ResponseEntity.status(HttpStatus.CREATED).body("Document uploaded successfully");
  }

  @PostMapping("/query")
  public ResponseEntity<String> query(@RequestParam("query") String query) {
    String answer = documentService.query(query);
    return ResponseEntity.ok(answer);
  }
}
