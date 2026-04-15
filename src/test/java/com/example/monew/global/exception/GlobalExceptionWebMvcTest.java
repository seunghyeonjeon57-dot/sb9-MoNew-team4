package com.example.monew.global.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = GlobalExceptionWebMvcTest.DummyController.class)
@Import({GlobalException.class, GlobalExceptionWebMvcTest.DummyController.class})
class GlobalExceptionWebMvcTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

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

  @Test
  @DisplayName("필수 헤더 누락 → 400 MISSING_REQUEST_HEADER + details.header")
  void missingRequiredHeader() throws Exception {
    mockMvc.perform(get("/_test/require-header"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MISSING_REQUEST_HEADER"))
        .andExpect(jsonPath("$.details.header").value("X-Required"));
  }

  @Test
  @DisplayName("@Valid 실패 → 400 INVALID_REQUEST + details.<field> 필드 에러")
  void validationFailure() throws Exception {
    String body = objectMapper.writeValueAsString(Map.of("name", ""));

    mockMvc.perform(post("/_test/validate")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
        .andExpect(jsonPath("$.details.name").exists());
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
    public Map<String, String> echo(@RequestBody Map<String, String> body) {
      return body;
    }

    @PostMapping("/_test/validate")
    public ValidatedRequest validate(@Valid @RequestBody ValidatedRequest body) {
      return body;
    }

    @GetMapping("/_test/require-header")
    public String requireHeader(
        @org.springframework.web.bind.annotation.RequestHeader("X-Required") String value) {
      return value;
    }

    record ValidatedRequest(@NotBlank String name) {}

    static class TestMonewException extends MonewException {
      TestMonewException() {
        super(ErrorCode.INTEREST_NOT_FOUND, Map.of("where", "integration"));
      }
    }
  }
}
