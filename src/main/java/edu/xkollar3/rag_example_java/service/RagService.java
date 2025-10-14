package edu.xkollar3.rag_example_java.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RagService {

  private final VectorStore vectorStore;
  private final ChatClient chatClient;

  // Prompt template for RAG
  private static final String PROMPT_TEMPLATE = """
      You are a helpful assistant that answers questions based on provided context.
      The context documents are in Czech language.

      Use the following pieces of context to answer the question at the end.
      If you don't find the answer in the context, say you don't know.
      Answer in a clear, human-readable way.

      Context:
      {context}

      Question: {question}

      Answer:
      """;

  private static final String QUESTION = "What is the insurance policy for the rent in our agreement, who is responsible for insuring the house? Will the owner insure my stuff at their expense?";

  public RagService(VectorStore vectorStore, ChatClient.Builder chatClientBuilder) {
    this.vectorStore = vectorStore;
    this.chatClient = chatClientBuilder.build();
  }

  public String askQuestion(String question, int topK) {
    List<Document> relevantDocs = vectorStore
        .similaritySearch(
            SearchRequest.builder().topK(5).query(QUESTION).build());

    log.info("retrieved: " + relevantDocs.size() + " relevant documents ");

    relevantDocs.forEach(doc -> log.info(doc.getText()));

    String context = relevantDocs.stream()
        .map(doc -> doc.getText())
        .collect(Collectors.joining("\n\n---\n\n"));

    PromptTemplate promptTemplate = new PromptTemplate(PROMPT_TEMPLATE);
    Prompt prompt = promptTemplate.create(Map.of(
        "context", context,
        "question", question));

    String response = chatClient.prompt(prompt).call().content();

    return response;
  }

  @PostConstruct
  public String askAboutInsurancePolicy() {
    log.info("=== Asking Question ===");
    log.info("Question: " + QUESTION);

    String answer = askQuestion(QUESTION, 5);

    log.info("=== Answer ===");
    log.info(answer);
    log.info("==================");

    return answer;
  }
}
