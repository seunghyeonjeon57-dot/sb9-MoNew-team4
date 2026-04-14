package com.example.monew.domain.interest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.monew.domain.interest.dto.InterestResponse;
import com.example.monew.domain.interest.dto.InterestUpdateRequest;
import com.example.monew.domain.interest.exception.InterestNotFoundException;
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
class InterestUpdateDeleteControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private InterestService interestService;

  @Test
  @DisplayName("PATCH /api/interests/{id}는 200과 갱신된 응답을 반환한다")
  void patchReturns200() throws Exception {
    UUID id = UUID.randomUUID();
    given(interestService.updateKeywords(eq(id), any(InterestUpdateRequest.class)))
        .willReturn(new InterestResponse(id, "인공지능", List.of("ML"), 3L, true));

    mockMvc.perform(patch("/api/interests/" + id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new InterestUpdateRequest(List.of("ML")))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.keywords[0]").value("ML"));
  }

  @Test
  @DisplayName("PATCH: 키워드 리스트가 비면 400")
  void patchReturns400WhenEmpty() throws Exception {
    mockMvc.perform(patch("/api/interests/" + UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new InterestUpdateRequest(List.of()))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details.fieldErrors.keywords").exists());
  }

  @Test
  @DisplayName("PATCH: 존재하지 않는 ID는 404")
  void patchReturns404() throws Exception {
    UUID id = UUID.randomUUID();
    given(interestService.updateKeywords(eq(id), any(InterestUpdateRequest.class)))
        .willThrow(new InterestNotFoundException(id));

    mockMvc.perform(patch("/api/interests/" + id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new InterestUpdateRequest(List.of("X")))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("INTEREST_NOT_FOUND"));
  }

  @Test
  @DisplayName("DELETE /api/interests/{id}는 204를 반환한다")
  void deleteReturns204() throws Exception {
    UUID id = UUID.randomUUID();

    mockMvc.perform(delete("/api/interests/" + id))
        .andExpect(status().isNoContent());

    verify(interestService).delete(id);
  }

  @Test
  @DisplayName("DELETE: 존재하지 않는 ID는 404")
  void deleteReturns404() throws Exception {
    UUID id = UUID.randomUUID();
    doThrow(new InterestNotFoundException(id)).when(interestService).delete(id);

    mockMvc.perform(delete("/api/interests/" + id))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("INTEREST_NOT_FOUND"));
  }
}
