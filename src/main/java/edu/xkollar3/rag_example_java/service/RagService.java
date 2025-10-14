package edu.xkollar3.rag_example_java.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RagService {

  private final VectorStore vectorStore;
  private final ChatClient chatClient;

  private static final String PROMPT_TEMPLATE = """
      You are a helpful assistant that answers questions based on provided context.

      Use the following pieces of context to answer the question at the end.
      If you don't find the answer in the context, say you don't know.
      Answer in a clear, human-readable way.

      Context:
      {context}

      Question: {question}

      Answer:
      """;

  public RagService(VectorStore vectorStore, ChatClient.Builder chatClientBuilder) {
    this.vectorStore = vectorStore;
    this.chatClient = chatClientBuilder.build();
  }

  public String askQuestion(String question) {
    List<Document> relevantDocs = vectorStore
        .similaritySearch(
            SearchRequest.builder().topK(10).query(question).build());

    log.info("retrieved: " + relevantDocs.size() + " relevant documents ");

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

}
