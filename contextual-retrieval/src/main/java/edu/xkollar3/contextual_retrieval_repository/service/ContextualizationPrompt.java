package edu.xkollar3.contextual_retrieval_repository.service;

public class ContextualizationPrompt {

  public static final String PROMPT = """
      <document>
      {{WHOLE_DOCUMENT}}
      </document>
      Here is the chunk we want to situate within the whole document
      <chunk>
      {{CHUNK_CONTENT}}
      </chunk>
      Please give a short succinct context to situate this chunk within the overall document for the purposes of improving search retrieval of the chunk. Answer only with the succinct context and nothing else.
            """;
}
