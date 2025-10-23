package edu.xkollar3.rag_example_java.service.langchain;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.List;

interface QueryTransformer {

  @UserMessage("""
      You are an expert at rephrasing questions to improve document retrieval in a RAG system.
      Your task is to take a user's question and rephrase it into 5 specific, focused questions that would help retrieve relevant documents.

      Original question: {{question}}

      Generate 5 specific questions that:
      1. Break down the original question into different aspects
      2. Use synonyms and alternative phrasings
      3. Are specific enough to retrieve relevant documents
      4. Cover different angles of the user's intent

      Return the questions as a numbered list (1-5), one per line.
      """)
  String transformQuery(@V("question") String question);

}
