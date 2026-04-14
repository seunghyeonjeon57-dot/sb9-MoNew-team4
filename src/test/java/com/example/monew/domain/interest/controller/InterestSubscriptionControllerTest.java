package com.example.monew.domain.interest.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.monew.domain.interest.dto.SubscriptionDto;
import com.example.monew.domain.interest.exception.DuplicateSubscriptionException;
import com.example.monew.domain.interest.exception.SubscriptionNotFoundException;
import com.example.monew.domain.interest.service.InterestSubscriptionService;
import com.example.monew.global.exception.GlobalExceptionHandler;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InterestSubscriptionController.class)
@Import(GlobalExceptionHandler.class)
class InterestSubscriptionControllerTest {

  private static final String HEADER = "MoNew-Request-User-ID";

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private InterestSubscriptionService service;

  @Test
  @DisplayName("POST /api/interests/{id}/subscriptions는 201과 SubscriptionDto를 반환")
  void subscribeReturns201() throws Exception {
    UUID interestId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    given(service.subscribe(eq(interestId), eq(userId)))
        .willReturn(new SubscriptionDto(UUID.randomUUID(), interestId, userId,
            LocalDateTime.now()));

    mockMvc.perform(post("/api/interests/" + interestId + "/subscriptions")
            .header(HEADER, userId.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.interestId").value(interestId.toString()))
        .andExpect(jsonPath("$.userId").value(userId.toString()));
  }

  @Test
  @DisplayName("헤더 누락은 400")
  void subscribeMissingHeader() throws Exception {
    mockMvc.perform(post("/api/interests/" + UUID.randomUUID() + "/subscriptions"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
  }

  @Test
  @DisplayName("중복 구독은 409")
  void subscribeDuplicate() throws Exception {
    UUID interestId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    given(service.subscribe(eq(interestId), eq(userId)))
        .willThrow(new DuplicateSubscriptionException(interestId, userId));

    mockMvc.perform(post("/api/interests/" + interestId + "/subscriptions")
            .header(HEADER, userId.toString()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("DUPLICATE_SUBSCRIPTION"));
  }

  @Test
  @DisplayName("DELETE는 204를 반환")
  void unsubscribeReturns204() throws Exception {
    UUID interestId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    mockMvc.perform(delete("/api/interests/" + interestId + "/subscriptions")
            .header(HEADER, userId.toString()))
        .andExpect(status().isNoContent());

    verify(service).unsubscribe(interestId, userId);
  }

  @Test
  @DisplayName("미구독 취소는 400")
  void unsubscribeNotSubscribed() throws Exception {
    UUID interestId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    doThrow(new SubscriptionNotFoundException(interestId, userId))
        .when(service).unsubscribe(interestId, userId);

    mockMvc.perform(delete("/api/interests/" + interestId + "/subscriptions")
            .header(HEADER, userId.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("SUBSCRIPTION_NOT_FOUND"));
  }
}
