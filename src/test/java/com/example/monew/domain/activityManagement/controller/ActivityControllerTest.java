package com.example.monew.domain.activityManagement.controller;


import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.monew.domain.activityManagement.dto.UserActivityDto;
import com.example.monew.domain.activityManagement.service.ActivityService;
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
  @DisplayName("유저 활동 내역 조회 API를 호출하면 상태코드 200과 함께 데이터 반환한다.")
  void getUserActivity_ReturnsOK() throws Exception{
    UUID userId = UUID.randomUUID();
    String expectedEmail = ("test@test.com");
    String expectedNickname = "test";
    LocalDateTime expectedCreatedAt = LocalDateTime.now();

    UserActivityDto mockResponse = new UserActivityDto(
        userId,
        expectedEmail,
        expectedNickname,
        expectedCreatedAt,
        List.of(),
        List.of(),
        List.of(),
        List.of()
    );

    given(activityService.getUserActivity(userId)).willReturn(mockResponse);

    mockMvc.perform(get("/api/user-activities/{userId}", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(userId.toString())) // JSON 응답 필드 검증
        .andExpect(jsonPath("$.email").value(expectedEmail))
        .andExpect(jsonPath("$.nickname").value(expectedNickname))
        .andExpect(jsonPath("$.subscribedInterests").isArray()) // 배열 형태인지 검증
        .andExpect(jsonPath("$.subscribedInterests").isEmpty()); // 비어있는지 검증
  }
}
