package edu.xkollar3.rag_example_java.service;

import org.springframework.ai.document.Document;

import java.io.IOException;
import java.util.List;

import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import edu.xkollar3.rag_example_java.repository.DocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class DocumentService {

  private final VectorStore store;
  private final DocumentRepository documentRepository;

  @Autowired
  public DocumentService(VectorStore store, DocumentRepository documentRepository) {
    this.store = store;
    this.documentRepository = documentRepository;
  }

  public void loadDocumentUnderName(MultipartFile file, String documentName) throws IOException {
    PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(
        file.getResource(),
        PdfDocumentReaderConfig.builder()
            .withPagesPerDocument(1)
            .build());

    List<Document> pages = pdfReader.read();

    TokenTextSplitter splitter = TokenTextSplitter.builder()
        .withChunkSize(1200)
        .withMinChunkSizeChars(300)
        .withMinChunkLengthToEmbed(10)
        .withMaxNumChunks(10000)
        .withKeepSeparator(true)
        .build();

    List<Document> docs = splitter.split(pages);

    docs.forEach(doc -> doc.getMetadata().put("document_name", documentName));

    store.accept(docs);
  }

  public void deleteByDocumentName(String documentName) {
    log.info("Deleting documents with document_name: {}", documentName);
    documentRepository.deleteByDocumentName(documentName);
    log.info("Successfully deleted documents with document_name: {}", documentName);
  }

  @Transactional
  public void reindexDocument(MultipartFile file, String documentName) throws IOException {
    log.info("Reindexing document: {}", documentName);
    deleteByDocumentName(documentName);
    loadDocumentUnderName(file, documentName);
    log.info("Successfully reindexed document: {}", documentName);
  }

}
