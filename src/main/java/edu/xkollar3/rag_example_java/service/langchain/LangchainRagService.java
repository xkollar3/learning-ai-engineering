package edu.xkollar3.rag_example_java.service.langchain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import edu.xkollar3.rag_example_java.service.RagService;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

@Component
@Slf4j
@ConditionalOnProperty(value = "rag-service", havingValue = "langchain")
public class LangchainRagService implements RagService {

  private final EmbeddingStore<TextSegment> embeddingStore;
  private final EmbeddingModel embeddingModel;
  private final ChatModel chatModel;
  private final PromptRelevanceChecker relevanceChecker;
  private final QueryTransformer queryTransformer;

  @Autowired
  public LangchainRagService(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel,
      ChatModel chatModel) {
    this.embeddingStore = embeddingStore;
    this.embeddingModel = embeddingModel;
    this.chatModel = chatModel;
    this.relevanceChecker = AiServices.builder(PromptRelevanceChecker.class)
        .chatModel(chatModel)
        .build();
    this.queryTransformer = AiServices.builder(QueryTransformer.class)
        .chatModel(chatModel)
        .build();
  }

  @Override
  public String askQuestion(String question) {
    log.info("Processing question: {}", question);

    // Step 1: Transform the original question into 5 specific questions
    log.info("Transforming question into multiple specific queries");
    String transformedQueriesResponse = queryTransformer.transformQuery(question);
    List<String> transformedQueries = parseTransformedQueries(transformedQueriesResponse);
    log.info("Generated {} transformed queries from original question", transformedQueries.size());
    for (int i = 0; i < transformedQueries.size(); i++) {
      log.info("Transformed query {}: {}", i + 1, transformedQueries.get(i));
    }

    // Step 2: Retrieve documents for all transformed queries
    ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
        .embeddingStore(this.embeddingStore)
        .embeddingModel(this.embeddingModel)
        .maxResults(5) // on each interaction we will retrieve the 5 most relevant segments
        .build();

    log.info("Retrieving relevant segments from embedding store for all transformed queries");
    Set<String> uniqueChunks = new HashSet<>();
    for (String transformedQuery : transformedQueries) {
      List<Content> relevantContents = contentRetriever.retrieve(Query.from(transformedQuery));
      relevantContents.forEach(content -> log.info("content from question: " + content.textSegment().text()));

      for (Content content : relevantContents) {
        String chunk = content.textSegment().text();
        boolean isRelevant = relevanceChecker.isTextRelevant(question, chunk);

        if (isRelevant) {
          log.info("relevant chunk added: {}", chunk.substring(0, Math.min(100, chunk.length())));
          uniqueChunks.add(chunk);
        }
      }
    }

    List<String> relevantChunks = new ArrayList<>(uniqueChunks);
    log.info("Retrieved and filtered to {} unique relevant chunks from all transformed queries", relevantChunks.size());

    if (relevantChunks.size() == 0) {
      throw new IllegalStateException("Must find at least a single relevant chunk");
    }

    String context = String.join("\n\n", relevantChunks);
    log.info("Context assembled, size: {} characters", context.length());

    // Construct the final prompt with context and question
    String finalPrompt = String.format("""
        You are a helpful assistant that answers questions based on the provided context.
        Use ONLY the information from the context below to answer the question.
        If the context doesn't contain enough information to answer the question completely, say so.

        Context:
        %s

        Question: %s

        Answer:
        """, context, question);

    log.info("Generating answer using chat model");
    // Generate the answer using the chat model
    String answer = chatModel.chat(finalPrompt);
    log.info("Answer generated, length: {} characters", answer.length());

    return answer;
  }

  /**
   * Parses the transformed queries from the AI service response.
   * Expected format: numbered list (1-5) with one query per line.
   *
   * @param response the response from the QueryTransformer service
   * @return a list of parsed query strings
   */
  private List<String> parseTransformedQueries(String response) {
    List<String> queries = new ArrayList<>();
    String[] lines = response.split("\n");

    for (String line : lines) {
      String trimmed = line.trim();
      if (trimmed.isEmpty()) {
        continue;
      }

      // Remove numbering (e.g., "1. ", "2. ", etc.)
      String query = trimmed.replaceAll("^\\d+\\.\\s*", "");

      if (!query.isEmpty()) {
        queries.add(query);
      }
    }

    return queries;
  }
}
