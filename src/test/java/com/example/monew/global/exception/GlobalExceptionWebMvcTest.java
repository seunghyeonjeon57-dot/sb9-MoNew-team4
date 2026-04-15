package com.example.monew.global.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = GlobalExceptionWebMvcTest.DummyController.class)
@Import({GlobalException.class, GlobalExceptionWebMvcTest.DummyController.class})
class GlobalExceptionWebMvcTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  @DisplayName("MonewException → ErrorCode.status + 응답 본문 매핑")
  void monewExceptionMapped() throws Exception {
    mockMvc.perform(get("/_test/throw-monew"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("INTEREST_NOT_FOUND"))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.details.where").value("integration"));
  }

  @Test
  @DisplayName("처리되지 않은 RuntimeException → 500 fallback")
  void fallback() throws Exception {
    mockMvc.perform(get("/_test/throw-runtime"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.status").value(500));
  }

  @RestController
  static class DummyController {

    @GetMapping("/_test/throw-monew")
    public void throwMonew() {
      throw new TestMonewException();
    }

    @GetMapping("/_test/throw-runtime")
    public void throwRuntime() {
      throw new IllegalStateException("boom");
    }

    @PostMapping("/_test/echo")
    public Map<String, String> echo(@org.springframework.web.bind.annotation.RequestBody Map<String, String> body) {
      return body;
    }

    static class TestMonewException extends MonewException {
      TestMonewException() {
        super(ErrorCode.INTEREST_NOT_FOUND, Map.of("where", "integration"));
      }
    }
  }
}
