package edu.xkollar3.rag_example_java.service.langchain;

import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import dev.langchain4j.community.data.document.graph.GraphDocument;
import dev.langchain4j.community.data.document.transformer.graph.GraphTransformer;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import edu.xkollar3.rag_example_java.service.DocumentService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@ConditionalOnProperty(value = "documentService", havingValue = "langchain")
public class LangchainDocumentService implements DocumentService {

  private final EmbeddingStore<TextSegment> embeddingStore;
  private final GraphTransformer documentTransformer;

  @Autowired
  public LangchainDocumentService(EmbeddingStore<TextSegment> embeddingStore, GraphTransformer documentTransformer) {
    this.embeddingStore = embeddingStore;
    this.documentTransformer = documentTransformer;
  }

  @Override
  public void loadDocument(MultipartFile file, String documentName) throws IOException {
    Document document = Document.from(new String(file.getBytes(), "UTF-8"));

    GraphDocument graph = documentTransformer.transform(document);

    graph.nodes().forEach(node -> log.info(node.toString()));
    graph.relationships().forEach(edge -> log.info(edge.toString()));
    log.info(graph.toString());
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
