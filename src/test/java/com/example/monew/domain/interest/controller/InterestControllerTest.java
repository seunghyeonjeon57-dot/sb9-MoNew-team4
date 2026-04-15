package com.example.monew.domain.interest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.monew.domain.interest.dto.InterestCreateRequest;
import com.example.monew.domain.interest.dto.InterestResponse;
import com.example.monew.domain.interest.dto.InterestUpdateRequest;
import com.example.monew.domain.interest.exception.InterestNotFoundException;
import com.example.monew.domain.interest.exception.InvalidSortParameterException;
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
            .header("MoNew-Request-User-ID", UUID.randomUUID().toString())
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
            .header("MoNew-Request-User-ID", UUID.randomUUID().toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("SIMILAR_INTEREST_NAME"))
        .andExpect(jsonPath("$.details.existing").value("인공지능"));
  }

  @Test
  @DisplayName("PATCH /api/interests/{id}: 키워드 수정 → 200 + 응답")
  void patch200() throws Exception {
    UUID id = UUID.randomUUID();
    when(interestService.updateKeywords(eq(id), any(InterestUpdateRequest.class)))
        .thenReturn(new InterestResponse(id, "인공지능", List.of("ML", "DL"), 0L, false));

    String body = objectMapper.writeValueAsString(Map.of("keywords", List.of("ML", "DL")));

    mockMvc.perform(patch("/api/interests/" + id)
            .header("MoNew-Request-User-ID", UUID.randomUUID().toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.keywords[0]").value("ML"))
        .andExpect(jsonPath("$.keywords[1]").value("DL"));
  }

  @Test
  @DisplayName("PATCH /api/interests/{id}: 미존재 → 404 INTEREST_NOT_FOUND")
  void patch404() throws Exception {
    UUID id = UUID.randomUUID();
    when(interestService.updateKeywords(eq(id), any(InterestUpdateRequest.class)))
        .thenThrow(new InterestNotFoundException(Map.of("interestId", id.toString())));

    String body = objectMapper.writeValueAsString(Map.of("keywords", List.of("ML")));

    mockMvc.perform(patch("/api/interests/" + id)
            .header("MoNew-Request-User-ID", UUID.randomUUID().toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("INTEREST_NOT_FOUND"));
  }

  @Test
  @DisplayName("DELETE /api/interests/{id}: 성공 → 204")
  void delete204() throws Exception {
    UUID id = UUID.randomUUID();

    mockMvc.perform(delete("/api/interests/" + id)
            .header("MoNew-Request-User-ID", UUID.randomUUID().toString()))
        .andExpect(status().isNoContent());

    verify(interestService).delete(id);
  }

  @Test
  @DisplayName("DELETE /api/interests/{id}: 미존재 → 404 INTEREST_NOT_FOUND")
  void delete404() throws Exception {
    UUID id = UUID.randomUUID();
    doThrow(new InterestNotFoundException(Map.of("interestId", id.toString())))
        .when(interestService).delete(id);

    mockMvc.perform(delete("/api/interests/" + id)
            .header("MoNew-Request-User-ID", UUID.randomUUID().toString()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("INTEREST_NOT_FOUND"));
  }

  @Test
  @DisplayName("GET /api/interests: 기본 → 200 + 리스트")
  void list200() throws Exception {
    UUID id = UUID.randomUUID();
    when(interestService.getInterests(any(), any(), any()))
        .thenReturn(List.of(new InterestResponse(id, "AI", List.of("ai"), 0L, false)));

    mockMvc.perform(get("/api/interests"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("AI"));
  }

  @Test
  @DisplayName("GET /api/interests?sortBy=foo → 400 INVALID_SORT_PARAMETER")
  void listInvalidSort400() throws Exception {
    when(interestService.getInterests(eq("foo"), any(), any()))
        .thenThrow(new InvalidSortParameterException(Map.of("sortBy", "foo")));

    mockMvc.perform(get("/api/interests").param("sortBy", "foo"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_SORT_PARAMETER"));
  }

  @Test
  @DisplayName("POST /api/interests: name blank → 400 INVALID_REQUEST")
  void createBlankName400() throws Exception {
    String body = objectMapper.writeValueAsString(
        Map.of("name", "", "keywords", List.of("AI")));

    mockMvc.perform(post("/api/interests")
            .header("MoNew-Request-User-ID", UUID.randomUUID().toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
  }

  @Test
  @DisplayName("POST /api/interests: 헤더 누락 → 400 MISSING_REQUEST_HEADER")
  void createMissingHeader400() throws Exception {
    String body = objectMapper.writeValueAsString(
        Map.of("name", "인공지능", "keywords", List.of("AI")));

    mockMvc.perform(post("/api/interests")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MISSING_REQUEST_HEADER"))
        .andExpect(jsonPath("$.details.header").value("MoNew-Request-User-ID"));
  }

  @Test
  @DisplayName("PATCH /api/interests/{id}: 헤더 누락 → 400 MISSING_REQUEST_HEADER")
  void patchMissingHeader400() throws Exception {
    String body = objectMapper.writeValueAsString(Map.of("keywords", List.of("ML")));

    mockMvc.perform(patch("/api/interests/" + UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MISSING_REQUEST_HEADER"))
        .andExpect(jsonPath("$.details.header").value("MoNew-Request-User-ID"));
  }

  @Test
  @DisplayName("DELETE /api/interests/{id}: 헤더 누락 → 400 MISSING_REQUEST_HEADER")
  void deleteMissingHeader400() throws Exception {
    mockMvc.perform(delete("/api/interests/" + UUID.randomUUID()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MISSING_REQUEST_HEADER"))
        .andExpect(jsonPath("$.details.header").value("MoNew-Request-User-ID"));
  }
}
