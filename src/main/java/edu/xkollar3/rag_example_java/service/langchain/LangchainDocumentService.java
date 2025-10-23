package edu.xkollar3.rag_example_java.service.langchain;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import edu.xkollar3.rag_example_java.service.DocumentService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@ConditionalOnProperty(value = "document-service", havingValue = "langchain")
public class LangchainDocumentService implements DocumentService {

  private final EmbeddingStoreIngestor ingestor;

  @Autowired
  public LangchainDocumentService(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) {
    this.ingestor = EmbeddingStoreIngestor.builder()
        .embeddingStore(embeddingStore)
        .embeddingModel(embeddingModel)
        .documentSplitter(DocumentSplitters.recursive(2000, 400)).build();

  }

  @Override
  public void loadDocument(MultipartFile file, String documentName) throws IOException {
    ApacheTikaDocumentParser parser = new ApacheTikaDocumentParser();
    Document document = parser.parse(file.getInputStream());

    DocumentSplitter splitter = DocumentSplitters.recursive(2000, 400);
    List<TextSegment> textSegments = splitter.split(document);
    textSegments.forEach(segment -> log.info(segment.text()));

    log.info("Ingesting document, size: " + document.text().length());
    this.ingestor.ingest(document);
  }

  @Override
  public void reindexDocument(MultipartFile file, String documentName) throws IOException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'reindexDocument'");
  }

  @Override
  public void deleteDocument(String documentName) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'deleteDocument'");
  }
}
