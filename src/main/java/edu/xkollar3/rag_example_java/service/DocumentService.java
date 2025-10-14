package edu.xkollar3.rag_example_java.service;

import org.springframework.ai.document.Document;
import java.util.List;

import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DocumentService {

  @Value("classpath:/static/agreement.pdf")
  private Resource pdfResource;

  private final VectorStore store;

  @Autowired
  public DocumentService(VectorStore store) {
    this.store = store;
  }

  public void load() {
    log.info("loading pdf");
    PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(this.pdfResource,
        PdfDocumentReaderConfig.builder().build());

    TokenTextSplitter splitter = new TokenTextSplitter();

    List<Document> docs = splitter.split(pdfReader.read());

    this.store.accept(docs);
    log.info("loaded pdf into pg vector store");
  }

}
