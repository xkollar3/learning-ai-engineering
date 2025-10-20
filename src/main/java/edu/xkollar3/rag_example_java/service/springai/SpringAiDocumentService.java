package edu.xkollar3.rag_example_java.service.springai;

import org.springframework.ai.document.Document;

import java.io.IOException;
import java.util.List;

import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import edu.xkollar3.rag_example_java.repository.DocumentRepository;
import edu.xkollar3.rag_example_java.service.DocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@ConditionalOnProperty(value = "documentService", havingValue = "spring")
public class SpringAiDocumentService implements DocumentService {

  private final VectorStore store;
  private final DocumentRepository documentRepository;

  @Autowired
  public SpringAiDocumentService(VectorStore store, DocumentRepository documentRepository) {
    this.store = store;
    this.documentRepository = documentRepository;
  }

  /**
   * In here we do completely naive loading, not preserving the semantic structure
   * of the document
   *
   * When testing RAG with this type of loading I encountered various limitations
   * because of missing semantics of the chunks
   *
   * For example I asked a question about a paragraph which is split across two
   * pages, depending on formulation of the question
   * the answer vector would at least be close to one of the chunks but sometimes
   * miss the second relevant chunk when going for exactly 2 chunks
   * A dirty solution for this was to get more chunks (5 for example) which would
   * always contain the relevant chunks among the other irrelevant ones
   *
   * I will try to solve this better with langchain API to preserve semantics of
   * the document so that relevant paragraphs of texts are related to eachother
   *
   * Another more advanced technique that could help would be query transformation
   * which I want to try later
   **/
  public void loadDocument(MultipartFile file, String documentName) throws IOException {
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

  public void deleteDocument(String documentName) {
    log.info("Deleting documents with document_name: {}", documentName);
    documentRepository.deleteByDocumentName(documentName);
    log.info("Successfully deleted documents with document_name: {}", documentName);
  }

  @Transactional
  public void reindexDocument(MultipartFile file, String documentName) throws IOException {
    log.info("Reindexing document: {}", documentName);
    deleteDocument(documentName);
    loadDocument(file, documentName);
    log.info("Successfully reindexed document: {}", documentName);
  }

}
