package com.example.monew.domain.interest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.monew.domain.interest.dto.CursorSlice;
import com.example.monew.domain.interest.dto.InterestResponse;
import com.example.monew.domain.interest.service.InterestService;
import com.example.monew.global.exception.GlobalExceptionHandler;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InterestController.class)
@Import(GlobalExceptionHandler.class)
class InterestListControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private InterestService interestService;

  @Test
  @DisplayName("GET /api/interests 기본 호출은 200과 CursorSlice 형태를 반환한다")
  void getReturns200AndSlice() throws Exception {
    UUID id = UUID.randomUUID();
    given(interestService.getInterests(
        eq(null), eq("name"), eq("asc"), eq(null), eq(20), any()))
        .willReturn(new CursorSlice<>(
            List.of(new InterestResponse(id, "관심사A", List.of("k"), 0L, false)),
            null, false, 1L));

    mockMvc.perform(get("/api/interests"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(id.toString()))
        .andExpect(jsonPath("$.content[0].name").value("관심사A"))
        .andExpect(jsonPath("$.hasNext").value(false))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  @DisplayName("쿼리 파라미터(keyword/sortBy/direction/cursor/size)는 서비스에 그대로 전달된다")
  void getPassesQueryParams() throws Exception {
    given(interestService.getInterests(
        eq("AI"), eq("subscriberCount"), eq("desc"), eq("10|abc"), eq(5), any()))
        .willReturn(new CursorSlice<>(List.of(), null, false, 0L));

    mockMvc.perform(get("/api/interests")
            .param("keyword", "AI")
            .param("sortBy", "subscriberCount")
            .param("direction", "desc")
            .param("cursor", "10|abc")
            .param("size", "5"))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("MoNew-Request-User-ID 헤더가 있으면 해당 userId로 서비스가 호출된다")
  void getForwardsUserIdHeader() throws Exception {
    UUID userId = UUID.randomUUID();
    given(interestService.getInterests(
        any(), any(), any(), any(), any(Integer.class), eq(userId)))
        .willReturn(new CursorSlice<>(List.of(), null, false, 0L));

    mockMvc.perform(get("/api/interests")
            .header("MoNew-Request-User-ID", userId.toString()))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("sortBy 허용값 외 값은 IllegalArgumentException → 400 INVALID_REQUEST")
  void getReturns400WhenSortByInvalid() throws Exception {
    given(interestService.getInterests(
        any(), eq("invalid"), any(), any(), any(Integer.class), any()))
        .willThrow(new IllegalArgumentException("sortBy must be one of [name, subscriberCount] but was: invalid"));

    mockMvc.perform(get("/api/interests").param("sortBy", "invalid"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
        .andExpect(jsonPath("$.details.reason").value(
            org.hamcrest.Matchers.containsString("sortBy")));
  }

  @Test
  @DisplayName("direction 허용값 외 값은 IllegalArgumentException → 400 INVALID_REQUEST")
  void getReturns400WhenDirectionInvalid() throws Exception {
    given(interestService.getInterests(
        any(), any(), eq("sideways"), any(), any(Integer.class), any()))
        .willThrow(new IllegalArgumentException("direction must be one of [asc, desc] but was: sideways"));

    mockMvc.perform(get("/api/interests").param("direction", "sideways"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details.reason").value(
            org.hamcrest.Matchers.containsString("direction")));
  }
}
