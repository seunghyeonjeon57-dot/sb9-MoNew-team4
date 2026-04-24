package com.example.monew.domain.notification.controller;

import com.example.monew.domain.notification.service.NotificationService;
import com.example.monew.global.exception.GlobalException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@Import(GlobalException.class)
class NotificationControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private NotificationService notificationService;

  private static final String USER_ID_HEADER = "Monew-Request-User-ID";
  private static final String VALID_USER_ID = UUID.randomUUID().toString();

  @Test
  @DisplayName("알림 목록 조회 시 헤더가 없으면 400 에러를 반환한다.")
  void getNotifications_WithoutHeader() throws Exception {
    mockMvc.perform(get("/api/notifications"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("알림 목록 조회 시 헤더가 있으면 200 OK를 반환한다.")
  void getNotifications_WithHeader() throws Exception {
    mockMvc.perform(get("/api/notifications")
            .header(USER_ID_HEADER, VALID_USER_ID))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("단건 알림 확인 시 헤더가 없으면 400 에러를 반환한다.")
  void confirmNotification_WithoutHeader() throws Exception {
    UUID notificationId = UUID.randomUUID();

    mockMvc.perform(patch("/api/notifications/{notificationId}", notificationId))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("단건 알림 확인 시 헤더가 있으면 200 OK를 반환한다.")
  void confirmNotification_WithHeader() throws Exception {
    UUID notificationId = UUID.randomUUID();

    mockMvc.perform(patch("/api/notifications/{notificationId}", notificationId)
            .header(USER_ID_HEADER, VALID_USER_ID))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("전체 알림 확인 시 헤더가 없으면 400 에러를 반환한다.")
  void confirmAllNotifications_WithoutHeader() throws Exception {
    mockMvc.perform(patch("/api/notifications"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("전체 알림 확인 시 헤더가 있으면 200 OK를 반환한다.")
  void confirmAllNotifications_WithHeader() throws Exception {
    mockMvc.perform(patch("/api/notifications")
            .header(USER_ID_HEADER, VALID_USER_ID))
        .andExpect(status().isOk());
  }
}
