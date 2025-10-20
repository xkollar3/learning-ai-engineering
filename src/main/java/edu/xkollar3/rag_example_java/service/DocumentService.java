package edu.xkollar3.rag_example_java.service;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

public interface DocumentService {
  void loadDocument(MultipartFile file, String documentName) throws IOException;

  void reindexDocument(MultipartFile file, String documentName) throws IOException;

  void deleteDocument(String documentName);
}
