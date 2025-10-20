package edu.xkollar3.rag_example_java.service.langchain;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import lombok.extern.slf4j.Slf4j;
import dev.langchain4j.community.data.document.transformer.graph.GraphTransformer;
import dev.langchain4j.community.data.document.transformer.graph.LLMGraphTransformer;

@Configuration
@Slf4j
public class TransformerConfiguration {

  @Bean
  public GraphTransformer graphTransformer(@Value("${openai-api-key}") String apiKey) {
    if (apiKey.isEmpty() || apiKey == null) {
      throw new IllegalStateException("openai-api-key must be defined");
    }

    List<String> allowedNodes = List.of("root", "section", "subsection");
    List<String> allowedRelations = List.of("HAS_SECTION", "HAS_SUBSECTION");
    String additionalInstructions = """
            You are parsing a legal or business agreement.

            - The document is structured with a bold heading/title at the top.
            - Followed by numbered sections
            - Create a graph structure where:
              - There is a single root node.
              - Each section might be numbered with numerals or roman numerals
              - Under a section there will be subsections that are again numbered via numbers or roman numerals
              - If there is more deeper nesting consider it part of subsection
              - Sections are connected to the root node with the `HAS_SECTION` relationship.
              - Subsections are connected to their parent section with the `HAS_SUBSECTION` relationship.
            - If there are parts of the document that do not have subsections (e.g. signature blocks, personal info), represent them as a `Section` node directly under the root.

            The goal is to reflect the document’s hierarchy clearly in the graph.
        """;

    String example = """
        1. Definitions

        I. “Agreement” means this document and all its appendices.
        II. “Party” refers to any entity entering into this Agreement.
            """;
    return new LLMGraphTransformer(
        OpenAiChatModel.builder().modelName(OpenAiChatModelName.GPT_4_1_MINI).apiKey(apiKey).build(), allowedNodes,
        allowedRelations,
        null, additionalInstructions, example, 3);
  }
}
