package edu.xkollar3.rag_example_java.service.langchain;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

interface PromptRelevanceChecker {

  @UserMessage("""
      You are a relevance evaluator for a Retrieval-Augmented Generation (RAG) system.
      Your task is to determine if a given piece of text contains information that can help answer a specific question.

      Question: {{question}}

      Text to evaluate:
      {{text}}

      Analyze the text carefully and determine if it contains relevant information to answer the question.
      Return true ONLY if the text directly contains information that helps answer the question.
      Return false if the text is only tangentially related, off-topic, or does not provide useful information for answering the question.

      IMPORTANT: Be strict in your evaluation. Only return true if the text genuinely helps answer the question.
      """)
  boolean isTextRelevant(@V("question") String question, @V("text") String text);

}
