package com.example.monew.domain.activity.controller;


import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.monew.domain.activity.dto.UserActivityDto;
import com.example.monew.domain.activity.service.ActivityService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ActivityController.class)
public class ActivityControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ActivityService activityService;

  @Test
  @DisplayName("사용자 활동 내역 조회 성공 시 200과 데이터를 반환한다")
  void getUserActivity_ReturnsOK() throws Exception {
    UUID userId = UUID.randomUUID();
    UserActivityDto mockResponse = UserActivityDto.builder()
        .id(userId)
        .email("test@test.com")
        .nickname("test")
        .createdAt(LocalDateTime.now())
        .subscriptions(List.of())
        .comments(List.of())
        .build();

    given(activityService.syncActivity(userId)).willReturn(mockResponse);

    mockMvc.perform(get("/api/user-activities/{userId}", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(userId.toString()))
        .andExpect(jsonPath("$.nickname").value("test"))
        .andDo(print());
  }
}
