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

import com.example.monew.domain.interest.dto.CursorPageResponse;
import com.example.monew.domain.interest.dto.InterestCreateRequest;
import com.example.monew.domain.interest.dto.InterestResponse;
import com.example.monew.domain.interest.dto.InterestUpdateRequest;
import com.example.monew.domain.interest.exception.InterestNotFoundException;
import com.example.monew.domain.interest.exception.InvalidSortParameterException;
import com.example.monew.domain.interest.exception.SimilarInterestNameException;
import com.example.monew.domain.interest.service.InterestService;
import com.example.monew.global.exception.GlobalException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.HashMap;
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
            .header("Monew-Request-User-ID", UUID.randomUUID().toString())
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
            .header("Monew-Request-User-ID", UUID.randomUUID().toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("SIMILAR_INTEREST_NAME"))
        .andExpect(jsonPath("$.details.existing").value("인공지능"));
  }

  @Test
  @DisplayName("PATCH /api/interests/{interestId}: 키워드 수정 → 200 + 응답 + userId 서비스 전달")
  void patch200() throws Exception {
    UUID id = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    when(interestService.updateKeywords(eq(id), any(InterestUpdateRequest.class), eq(userId)))
        .thenReturn(new InterestResponse(id, "인공지능", List.of("ML", "DL"), 0L, true));

    String body = objectMapper.writeValueAsString(Map.of("keywords", List.of("ML", "DL")));

    mockMvc.perform(patch("/api/interests/" + id)
            .header("Monew-Request-User-ID", userId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.keywords[0]").value("ML"))
        .andExpect(jsonPath("$.keywords[1]").value("DL"))
        .andExpect(jsonPath("$.subscribedByMe").value(true));

    verify(interestService).updateKeywords(eq(id), any(InterestUpdateRequest.class), eq(userId));
  }

  @Test
  @DisplayName("PATCH /api/interests/{interestId}: 미존재 → 404 INTEREST_NOT_FOUND")
  void patch404() throws Exception {
    UUID id = UUID.randomUUID();
    when(interestService.updateKeywords(eq(id), any(InterestUpdateRequest.class), any(UUID.class)))
        .thenThrow(new InterestNotFoundException(Map.of("interestId", id.toString())));

    String body = objectMapper.writeValueAsString(Map.of("keywords", List.of("ML")));

    mockMvc.perform(patch("/api/interests/" + id)
            .header("Monew-Request-User-ID", UUID.randomUUID().toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("INTEREST_NOT_FOUND"));
  }

  @Test
  @DisplayName("PATCH /api/interests/{interestId}: name 포함 → 400 INVALID_REQUEST (Bean Validation @Null 위반)")
  void patchNameImmutable400() throws Exception {
    UUID id = UUID.randomUUID();

    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("name", "바뀐이름");
    requestBody.put("keywords", List.of("ML"));
    String body = objectMapper.writeValueAsString(requestBody);

    mockMvc.perform(patch("/api/interests/" + id)
            .header("Monew-Request-User-ID", UUID.randomUUID().toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
  }

  @Test
  @DisplayName("DELETE /api/interests/{interestId}: 성공 → 204")
  void delete204() throws Exception {
    UUID id = UUID.randomUUID();

    mockMvc.perform(delete("/api/interests/" + id)
            .header("Monew-Request-User-ID", UUID.randomUUID().toString()))
        .andExpect(status().isNoContent());

    verify(interestService).delete(id);
  }

  @Test
  @DisplayName("DELETE /api/interests/{interestId}: 미존재 → 404 INTEREST_NOT_FOUND")
  void delete404() throws Exception {
    UUID id = UUID.randomUUID();
    doThrow(new InterestNotFoundException(Map.of("interestId", id.toString())))
        .when(interestService).delete(id);

    mockMvc.perform(delete("/api/interests/" + id)
            .header("Monew-Request-User-ID", UUID.randomUUID().toString()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("INTEREST_NOT_FOUND"));
  }

  @Test
  @DisplayName("GET /api/interests: required 파라미터 전부 포함 → 200 + CursorPageResponse")
  void list200() throws Exception {
    UUID id = UUID.randomUUID();
    CursorPageResponse<InterestResponse> pageResponse = new CursorPageResponse<>(
        List.of(new InterestResponse(id, "AI", List.of("ai"), 0L, false)),
        null, null, 20, 1L, false);
    when(interestService.getInterests(
        any(), any(), any(), any(), any(), any(Integer.class), any()))
        .thenReturn(pageResponse);

    mockMvc.perform(get("/api/interests")
            .header("Monew-Request-User-ID", UUID.randomUUID().toString())
            .param("orderBy", "name")
            .param("direction", "ASC")
            .param("limit", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].name").value("AI"))
        .andExpect(jsonPath("$.hasNext").value(false));
  }

  @Test
  @DisplayName("GET /api/interests?keyword=AI → keyword 파라미터를 서비스에 전달")
  void listKeywordParam() throws Exception {
    UUID id = UUID.randomUUID();
    CursorPageResponse<InterestResponse> pageResponse = new CursorPageResponse<>(
        List.of(new InterestResponse(id, "인공지능", List.of("AI"), 0L, false)),
        null, null, 20, 1L, false);
    when(interestService.getInterests(
        eq("AI"), any(), any(), any(), any(), any(Integer.class), any()))
        .thenReturn(pageResponse);

    mockMvc.perform(get("/api/interests")
            .header("Monew-Request-User-ID", UUID.randomUUID().toString())
            .param("keyword", "AI")
            .param("orderBy", "name")
            .param("direction", "ASC")
            .param("limit", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].name").value("인공지능"));
  }

  @Test
  @DisplayName("GET /api/interests?orderBy=foo → 400 INVALID_SORT_PARAMETER")
  void listInvalidSort400() throws Exception {
    when(interestService.getInterests(
        any(), eq("foo"), any(), any(), any(), any(Integer.class), any()))
        .thenThrow(new InvalidSortParameterException(Map.of("orderBy", "foo")));

    mockMvc.perform(get("/api/interests")
            .header("Monew-Request-User-ID", UUID.randomUUID().toString())
            .param("orderBy", "foo")
            .param("direction", "ASC")
            .param("limit", "20"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_SORT_PARAMETER"));
  }

  @Test
  @DisplayName("GET /api/interests?cursor=not-a-uuid → 400 INVALID_SORT_PARAMETER (silent null 제거)")
  void listInvalidCursor400() throws Exception {
    when(interestService.getInterests(
        any(), any(), any(), eq("not-a-uuid"), any(), any(Integer.class), any()))
        .thenThrow(new InvalidSortParameterException(Map.of("cursor", "not-a-uuid")));

    mockMvc.perform(get("/api/interests")
            .header("Monew-Request-User-ID", UUID.randomUUID().toString())
            .param("orderBy", "name")
            .param("direction", "ASC")
            .param("cursor", "not-a-uuid")
            .param("limit", "20"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_SORT_PARAMETER"));
  }

  @Test
  @DisplayName("GET /api/interests?cursor=&limit=20 → 커서 페이지네이션 응답")
  void listCursorPagination() throws Exception {
    UUID id = UUID.randomUUID();
    String nextCursor = UUID.randomUUID().toString();
    CursorPageResponse<InterestResponse> pageResponse = new CursorPageResponse<>(
        List.of(new InterestResponse(id, "AI", List.of("ai"), 0L, false)),
        nextCursor, LocalDateTime.now(), 20, 2L, true);
    when(interestService.getInterests(
        any(), any(), any(), any(), any(), any(Integer.class), any()))
        .thenReturn(pageResponse);

    mockMvc.perform(get("/api/interests")
            .header("Monew-Request-User-ID", UUID.randomUUID().toString())
            .param("orderBy", "name")
            .param("direction", "ASC")
            .param("cursor", "")
            .param("limit", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].name").value("AI"))
        .andExpect(jsonPath("$.nextCursor").value(nextCursor))
        .andExpect(jsonPath("$.hasNext").value(true));
  }

  @Test
  @DisplayName("GET /api/interests: required 파라미터 누락 → 400")
  void listMissingRequiredParam400() throws Exception {
    mockMvc.perform(get("/api/interests")
            .header("Monew-Request-User-ID", UUID.randomUUID().toString())
            .param("orderBy", "name"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
  }

  @Test
  @DisplayName("GET /api/interests: 헤더 누락 → 400 MISSING_REQUEST_HEADER")
  void listMissingHeader400() throws Exception {
    mockMvc.perform(get("/api/interests")
            .param("orderBy", "name")
            .param("direction", "ASC")
            .param("limit", "20"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MISSING_REQUEST_HEADER"))
        .andExpect(jsonPath("$.details.header").value("Monew-Request-User-ID"));
  }

  @Test
  @DisplayName("POST /api/interests: name blank → 400 INVALID_REQUEST")
  void createBlankName400() throws Exception {
    String body = objectMapper.writeValueAsString(
        Map.of("name", "", "keywords", List.of("AI")));

    mockMvc.perform(post("/api/interests")
            .header("Monew-Request-User-ID", UUID.randomUUID().toString())
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
        .andExpect(jsonPath("$.details.header").value("Monew-Request-User-ID"));
  }

  @Test
  @DisplayName("PATCH /api/interests/{interestId}: 헤더 누락 → 400 MISSING_REQUEST_HEADER")
  void patchMissingHeader400() throws Exception {
    String body = objectMapper.writeValueAsString(Map.of("keywords", List.of("ML")));

    mockMvc.perform(patch("/api/interests/" + UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MISSING_REQUEST_HEADER"))
        .andExpect(jsonPath("$.details.header").value("Monew-Request-User-ID"));
  }

  @Test
  @DisplayName("DELETE /api/interests/{interestId}: 헤더 누락 → 400 MISSING_REQUEST_HEADER")
  void deleteMissingHeader400() throws Exception {
    mockMvc.perform(delete("/api/interests/" + UUID.randomUUID()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MISSING_REQUEST_HEADER"))
        .andExpect(jsonPath("$.details.header").value("Monew-Request-User-ID"));
  }
}
