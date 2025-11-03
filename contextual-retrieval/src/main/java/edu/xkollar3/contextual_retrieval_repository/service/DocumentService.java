package edu.xkollar3.contextual_retrieval_repository.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.ContentMetadata;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingStore;
import edu.xkollar3.contextual_retrieval_repository.model.DocumentChunkEntity;
import edu.xkollar3.contextual_retrieval_repository.model.DocumentEntity;
import edu.xkollar3.contextual_retrieval_repository.repository.DocumentChunkRepository;
import edu.xkollar3.contextual_retrieval_repository.repository.DocumentRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

// https://www.anthropic.com/engineering/contextual-retrieval
@Service
@Slf4j
public class DocumentService {

  private final DocumentRepository documentRepository;
  private final DocumentChunkRepository documentChunkRepository;
  private final EmbeddingStore<TextSegment> embeddingStore;
  private final EmbeddingModel embeddingModel;
  private final ContentRetriever contentRetriever;
  private final ChatModel chatModel;
  private final ExecutorService executorService = Executors
      .newVirtualThreadPerTaskExecutor();
  private final int K = 5;

  @Autowired
  public DocumentService(DocumentRepository documentRepository, DocumentChunkRepository documentChunkRepository,
      EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel, ContentRetriever contentRetriever,
      ChatModel chatModel) {
    this.documentRepository = documentRepository;
    this.documentChunkRepository = documentChunkRepository;
    this.embeddingStore = embeddingStore;
    this.embeddingModel = embeddingModel;
    this.contentRetriever = EmbeddingStoreContentRetriever.builder()
        .embeddingModel(embeddingModel)
        .embeddingStore(embeddingStore)
        .maxResults(K)
        .build();
    this.chatModel = chatModel;
  }

  // create TF IDF index and also embedding for document
  @Transactional
  public void uploadDocument(MultipartFile file, String documentName) throws IOException {
    log.info("Uploading document with name: {}", documentName);

    DocumentEntity entity = new DocumentEntity();
    entity.setName(documentName);
    documentRepository.save(entity);

    DocumentParser parser = new ApacheTikaDocumentParser();
    DocumentSplitter splitter = DocumentSplitters.recursive(400, 200);
    Document document = parser.parse(file.getInputStream());

    List<TextSegment> segments = splitter.split(document);
    String wholeDocumentText = document.text();

    List<Future<DocumentChunkEntity>> futures = new ArrayList<>();

    // Submit all tasks to executor service
    for (int chunkIndex = 0; chunkIndex < segments.size(); chunkIndex++) {
      final int index = chunkIndex;
      TextSegment segment = segments.get(chunkIndex);

      Future<DocumentChunkEntity> future = executorService.submit(() -> {
        String contextualizedChunk = contextualizeChunk(wholeDocumentText, segment.text());
        log.debug("Contextualized chunk: {}", contextualizedChunk);
        TextSegment contextualizedSegment = TextSegment.from(contextualizedChunk);

        // INEFFICIENT batch embed should be called! for demo is ok
        Embedding embed = embeddingModel.embed(contextualizedSegment).content();
        String id = embeddingStore.add(embed, contextualizedSegment);

        DocumentChunkEntity chunk = new DocumentChunkEntity();
        chunk.setDocument(entity);
        chunk.setContent(segment.text());
        chunk.setContextualizedText(contextualizedChunk);
        chunk.setChunkIndex(index);
        chunk.setEmbeddingId(UUID.fromString(id));
        return chunk;
      });

      futures.add(future);
    }

    List<DocumentChunkEntity> results = new ArrayList<>();

    // Wait for all tasks to complete
    for (Future<DocumentChunkEntity> future : futures) {
      try {
        DocumentChunkEntity chunk = future.get();
        results.add(chunk);
      } catch (Exception e) {
        log.error("Error processing chunk", e);
        throw new RuntimeException("Failed to process chunk", e);
      }
    }

    documentChunkRepository.saveAll(results);

    log.info("All {} chunks processed and saved to database", futures.size());
  }

  private String contextualizeChunk(String wholeDocument, String chunkContent) {
    log.debug("Contextualizing chunk with LLM");
    // TODO: replace document in earlier to avoid doing everytime
    String prompt = ContextualizationPrompt.PROMPT
        .replace("{{WHOLE_DOCUMENT}}", wholeDocument)
        .replace("{{CHUNK_CONTENT}}", chunkContent);

    String context = chatModel.chat(prompt);
    log.debug("Generated context: {}", context);

    return context + "\n\n" + chunkContent;
  }

  public String query(String query) {
    log.info("Processing query: {}", query);
    List<String> retrievedChunks = retrieveTopK(query);

    String context = String.join("\n\n", retrievedChunks);
    String prompt = String.format(
        "Use the following context to answer the question:\n\nContext:\n%s\n\nQuestion: %s",
        context,
        query);

    String answer = chatModel.chat(prompt);
    log.info("Generated answer for query: {}", query);
    return answer;
  }

  private List<String> retrieveTopK(String query) {
    log.info("Starting top-K retrieval for query: {}", query);
    Map<UUID, RetrievedChunk> fullTextSearchResults = score(doFulltextSearch(query));
    Map<UUID, RetrievedChunk> retrievalResults = score(doRetrieval(query));

    Set<UUID> uniqueIds = new HashSet<>();
    uniqueIds.addAll(fullTextSearchResults.keySet());
    uniqueIds.addAll(retrievalResults.keySet());

    log.info("Full-text search found {} unique chunks, vector retrieval found {} unique chunks, total unique: {}",
        fullTextSearchResults.size(), retrievalResults.size(), uniqueIds.size());

    // TreeMap sorted by score in descending order
    Map<UUID, RetrievedChunk> mergedResults = new TreeMap<>(
        Comparator.comparingDouble((UUID id) -> {
          float mergedScore = 0;
          if (fullTextSearchResults.containsKey(id)) {
            mergedScore += fullTextSearchResults.get(id).score();
          }
          if (retrievalResults.containsKey(id)) {
            mergedScore += retrievalResults.get(id).score();
          }
          return mergedScore;
        }).reversed());

    for (UUID id : uniqueIds) {
      float mergedScore = 0;
      String content = "";

      if (fullTextSearchResults.containsKey(id)) {
        RetrievedChunk chunk = fullTextSearchResults.get(id);
        mergedScore += chunk.score();
        content = chunk.content();
      }
      if (retrievalResults.containsKey(id)) {
        RetrievedChunk chunk = retrievalResults.get(id);
        mergedScore += chunk.score();
        content = chunk.content();
      }

      mergedResults.put(id, new RetrievedChunk(content, mergedScore));
      log.debug("Merged chunk ID: {} with score: {}", id, mergedScore);
    }

    List<String> topKChunks = mergedResults.entrySet().stream()
        .limit(K)
        .peek(entry -> log.info("Top-K retrieved chunk ID: {} with merged score: {}",
            entry.getKey(), String.format("%.4f", entry.getValue().score())))
        .map(entry -> entry.getValue().content())
        .toList();

    log.info("Retrieved top {} chunks for query", topKChunks.size());
    return topKChunks;

  }

  private Map<UUID, RetrievedChunk> score(Map<UUID, RetrievedChunk> items) {
    float K = 60;
    Map<UUID, RetrievedChunk> scored = new LinkedHashMap<>();
    float rank = 1;

    for (var entry : items.entrySet()) {
      float score = 1F / (rank + K);
      scored.put(entry.getKey(), new RetrievedChunk(entry.getValue().content(), score));
      rank++;
    }

    return scored;
  }

  private Map<UUID, RetrievedChunk> doFulltextSearch(String query) {
    log.info("Full text search with query: {}", query);
    Map<UUID, RetrievedChunk> results = new LinkedHashMap<>();
    for (DocumentChunkEntity entity : documentChunkRepository.searchByQuery(query, K)) {
      results.put(entity.getEmbeddingId(), new RetrievedChunk(entity.getContent(), -1F));
      log.debug("Full-text search found chunk ID: {}, content: {}",
          entity.getEmbeddingId(), entity.getContent());
    }
    log.info("Full-text search returned {} results", results.size());
    return results;
  }

  private Map<UUID, RetrievedChunk> doRetrieval(String query) {
    log.info("Doing vector similarity retrieval on query: {}", query);
    Map<UUID, RetrievedChunk> results = new LinkedHashMap<>();
    for (Content content : contentRetriever.retrieve(Query.from(query))) {
      String text = content.textSegment().text();
      String idString = ((String) content.metadata().get(ContentMetadata.EMBEDDING_ID));
      UUID chunkId = UUID.fromString(idString);
      results.put(chunkId, new RetrievedChunk(text, -1F));
      log.debug("Vector similarity search found chunk ID: {}, content: {}", chunkId, text);
    }
    log.info("Vector similarity retrieval returned {} results", results.size());
    return results;
  }
}
