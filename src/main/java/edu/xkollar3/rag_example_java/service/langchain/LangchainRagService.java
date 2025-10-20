package edu.xkollar3.rag_example_java.service.langchain;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import edu.xkollar3.rag_example_java.service.RagService;

@Component
@ConditionalOnProperty(value = "ragService", havingValue = "langchain")
public class LangchainRagService implements RagService {

  @Override
  public String askQuestion(String question) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'askQuestion'");
  }

}
