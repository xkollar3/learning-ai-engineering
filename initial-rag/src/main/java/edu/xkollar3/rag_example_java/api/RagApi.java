package edu.xkollar3.rag_example_java.api;

import edu.xkollar3.rag_example_java.service.RagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rag")
@Slf4j
public class RagApi {

  private final RagService ragService;

  @Autowired
  public RagApi(RagService ragService) {
    this.ragService = ragService;
  }

  @PostMapping("/ask")
  public ResponseEntity<AnswerDto> askQuestion(@RequestBody QuestionDto questionDto) {
    try {
      log.info("Received question: {}", questionDto.question());
      String answer = ragService.askQuestion(questionDto.question());
      return ResponseEntity.ok(new AnswerDto(answer));
    } catch (Exception e) {
      log.error("Error processing question: {}", e.getMessage());
      return ResponseEntity.badRequest().body(new AnswerDto("Error processing question: " + e.getMessage()));
    }
  }
}
