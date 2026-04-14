package com.example.monew.domain.interest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.monew.domain.interest.dto.InterestCreateRequest;
import com.example.monew.domain.interest.dto.InterestResponse;
import com.example.monew.domain.interest.exception.SimilarInterestNameException;
import com.example.monew.domain.interest.service.InterestService;
import com.example.monew.global.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InterestController.class)
@Import(GlobalExceptionHandler.class)
class InterestControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private InterestService interestService;

  @Test
  @DisplayName("POST /api/interests 정상 요청은 201과 InterestResponse를 반환한다")
  void createReturns201() throws Exception {
    UUID id = UUID.randomUUID();
    given(interestService.create(any(InterestCreateRequest.class)))
        .willReturn(new InterestResponse(id, "인공지능", List.of("AI"), 0L, false));

    mockMvc.perform(post("/api/interests")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new InterestCreateRequest("인공지능", List.of("AI")))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.name").value("인공지능"))
        .andExpect(jsonPath("$.subscribed").value(false));
  }

  @Test
  @DisplayName("이름이 비어있으면 400과 INVALID_REQUEST를 반환한다")
  void createReturns400WhenNameBlank() throws Exception {
    mockMvc.perform(post("/api/interests")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new InterestCreateRequest("", List.of("AI")))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
        .andExpect(jsonPath("$.details.fieldErrors.name").exists());
  }

  @Test
  @DisplayName("키워드 리스트가 비어있으면 400을 반환한다")
  void createReturns400WhenKeywordsEmpty() throws Exception {
    mockMvc.perform(post("/api/interests")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new InterestCreateRequest("인공지능", List.of()))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details.fieldErrors.keywords").exists());
  }

  @Test
  @DisplayName("유사 이름 등록 시 SimilarInterestNameException → 409")
  void createReturns409WhenSimilarName() throws Exception {
    given(interestService.create(any(InterestCreateRequest.class)))
        .willThrow(new SimilarInterestNameException("인공지능 기술", "인공지능", 0.85));

    mockMvc.perform(post("/api/interests")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new InterestCreateRequest("인공지능 기술", List.of("AI")))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("SIMILAR_INTEREST_NAME"))
        .andExpect(jsonPath("$.details.existing").value("인공지능"));
  }
}
