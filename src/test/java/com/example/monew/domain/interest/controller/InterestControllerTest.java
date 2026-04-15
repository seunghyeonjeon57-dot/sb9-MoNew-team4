package com.example.monew.domain.interest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.monew.domain.interest.dto.InterestCreateRequest;
import com.example.monew.domain.interest.dto.InterestResponse;
import com.example.monew.domain.interest.exception.SimilarInterestNameException;
import com.example.monew.domain.interest.service.InterestService;
import com.example.monew.global.exception.GlobalException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = InterestController.class)
@Import(GlobalException.class)
class InterestControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private InterestService interestService;

  @Test
  @DisplayName("POST /api/interests: 유효 요청 → 201 + 응답 바디")
  void create201() throws Exception {
    UUID id = UUID.randomUUID();
    when(interestService.create(any(InterestCreateRequest.class)))
        .thenReturn(new InterestResponse(id, "인공지능", List.of("AI", "ML"), 0L, false));

    String body = objectMapper.writeValueAsString(
        Map.of("name", "인공지능", "keywords", List.of("AI", "ML")));

    mockMvc.perform(post("/api/interests")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("인공지능"))
        .andExpect(jsonPath("$.keywords[0]").value("AI"));
  }

  @Test
  @DisplayName("POST /api/interests: 80% 유사 → 409 SIMILAR_INTEREST_NAME")
  void createSimilar409() throws Exception {
    when(interestService.create(any(InterestCreateRequest.class)))
        .thenThrow(new SimilarInterestNameException(Map.of("existing", "인공지능")));

    String body = objectMapper.writeValueAsString(
        Map.of("name", "인공지능A", "keywords", List.of("AI")));

    mockMvc.perform(post("/api/interests")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("SIMILAR_INTEREST_NAME"))
        .andExpect(jsonPath("$.details.existing").value("인공지능"));
  }

  @Test
  @DisplayName("POST /api/interests: name blank → 400 INVALID_REQUEST")
  void createBlankName400() throws Exception {
    String body = objectMapper.writeValueAsString(
        Map.of("name", "", "keywords", List.of("AI")));

    mockMvc.perform(post("/api/interests")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
  }
}
