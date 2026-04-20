package com.example.monew.domain.interest.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.monew.domain.interest.dto.SubscriptionResponse;
import com.example.monew.domain.interest.exception.DuplicateSubscriptionException;
import com.example.monew.domain.interest.exception.InterestNotFoundException;
import com.example.monew.domain.interest.exception.SubscriptionNotFoundException;
import com.example.monew.domain.interest.service.InterestSubscriptionService;
import com.example.monew.global.exception.GlobalException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = InterestSubscriptionController.class)
@Import(GlobalException.class)
class InterestSubscriptionControllerTest {

  private static final String USER_HEADER = "MoNew-Request-User-ID";

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private InterestSubscriptionService service;

  @Test
  @DisplayName("POST /api/interests/{id}/subscriptions: 성공 → 200 + SubscriptionDto")
  void subscribe200() throws Exception {
    UUID interestId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID subId = UUID.randomUUID();
    LocalDateTime createdAt = LocalDateTime.now();
    when(service.subscribe(eq(interestId), eq(userId)))
        .thenReturn(new SubscriptionResponse(
            subId, interestId, "인공지능", List.of("AI", "ML"), 3L, createdAt));

    mockMvc.perform(post("/api/interests/" + interestId + "/subscriptions")
            .header(USER_HEADER, userId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(subId.toString()))
        .andExpect(jsonPath("$.interestId").value(interestId.toString()))
        .andExpect(jsonPath("$.interestName").value("인공지능"))
        .andExpect(jsonPath("$.interestKeywords[0]").value("AI"))
        .andExpect(jsonPath("$.interestSubscriberCount").value(3));
  }

  @Test
  @DisplayName("POST /subscriptions: 헤더 누락 → 400 MISSING_REQUEST_HEADER")
  void subscribeMissingHeader400() throws Exception {
    mockMvc.perform(post("/api/interests/" + UUID.randomUUID() + "/subscriptions"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MISSING_REQUEST_HEADER"))
        .andExpect(jsonPath("$.details.header").value(USER_HEADER));
  }

  @Test
  @DisplayName("POST /subscriptions: 미존재 인터레스트 → 404")
  void subscribe404() throws Exception {
    UUID interestId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    when(service.subscribe(eq(interestId), eq(userId)))
        .thenThrow(new InterestNotFoundException(Map.of("interestId", interestId.toString())));

    mockMvc.perform(post("/api/interests/" + interestId + "/subscriptions")
            .header(USER_HEADER, userId.toString()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("INTEREST_NOT_FOUND"));
  }

  @Test
  @DisplayName("POST /subscriptions: 중복 → 409 DUPLICATE_SUBSCRIPTION")
  void subscribe409() throws Exception {
    UUID interestId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    when(service.subscribe(eq(interestId), eq(userId)))
        .thenThrow(new DuplicateSubscriptionException(Map.of("userId", userId.toString())));

    mockMvc.perform(post("/api/interests/" + interestId + "/subscriptions")
            .header(USER_HEADER, userId.toString()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("DUPLICATE_SUBSCRIPTION"));
  }

  @Test
  @DisplayName("DELETE /subscriptions: 성공 → 200")
  void unsubscribe200() throws Exception {
    UUID interestId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    mockMvc.perform(delete("/api/interests/" + interestId + "/subscriptions")
            .header(USER_HEADER, userId.toString()))
        .andExpect(status().isOk());

    verify(service).unsubscribe(interestId, userId);
  }

  @Test
  @DisplayName("DELETE /subscriptions: 헤더 누락 → 400")
  void unsubscribeMissingHeader400() throws Exception {
    mockMvc.perform(delete("/api/interests/" + UUID.randomUUID() + "/subscriptions"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MISSING_REQUEST_HEADER"));
  }

  @Test
  @DisplayName("DELETE /subscriptions: 미구독 → 404 SUBSCRIPTION_NOT_FOUND")
  void unsubscribe404() throws Exception {
    UUID interestId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    doThrow(new SubscriptionNotFoundException(Map.of("userId", userId.toString())))
        .when(service).unsubscribe(interestId, userId);

    mockMvc.perform(delete("/api/interests/" + interestId + "/subscriptions")
            .header(USER_HEADER, userId.toString()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("SUBSCRIPTION_NOT_FOUND"));
  }
}
