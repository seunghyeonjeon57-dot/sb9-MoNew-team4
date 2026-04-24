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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq; // 👈 올바른 eq import
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

    // 조회의 경우 getNotifications 메서드가 호출되었는지 검증
    verify(notificationService, times(1)).getNotifications(any(UUID.class), any(), any(), anyInt());
  }

  @Test
  @DisplayName("단건 알림 확인 시 헤더가 있으면 200 OK를 반환한다.")
  void confirmNotification_WithHeader() throws Exception {
    UUID notificationId = UUID.randomUUID();

    mockMvc.perform(patch("/api/notifications/{notificationId}", notificationId)
            .header(USER_ID_HEADER, VALID_USER_ID))
        .andExpect(status().isOk());

    // 단건 확인 서비스 호출 검증
    verify(notificationService, times(1)).confirmNotification(eq(notificationId), any(UUID.class));
  }

  @Test
  @DisplayName("전체 알림 확인 시 헤더가 있으면 200 OK를 반환한다.")
  void confirmAllNotifications_WithHeader() throws Exception {
    mockMvc.perform(patch("/api/notifications")
            .header(USER_ID_HEADER, VALID_USER_ID))
        .andExpect(status().isOk());

    // ✅ 전체 확인용 서비스 메서드(confirmAllNotifications)로 수정
    verify(notificationService, times(1)).confirmAllNotifications(any(UUID.class));
  }

  @Test
  @DisplayName("커서와 날짜 조건이 모두 있을 때 알림 목록을 조회한다.")
  void getNotifications_WithCursorAndAfter() throws Exception {
    mockMvc.perform(get("/api/notifications")
            .header(USER_ID_HEADER, VALID_USER_ID)
            .param("cursor", UUID.randomUUID().toString())
            .param("after", "2024-04-24T15:00:00")
            .param("limit", "10"))
        .andExpect(status().isOk());

    // 조회 서비스 호출 검증
    verify(notificationService, times(1)).getNotifications(any(UUID.class), any(String.class), any(), anyInt());
  }

  @Test
  @DisplayName("커서(cursor) 조건만 있을 때 알림 목록을 조회한다.")
  void getNotifications_WithCursorOnly() throws Exception {
    mockMvc.perform(get("/api/notifications")
            .header(USER_ID_HEADER, VALID_USER_ID)
            .param("cursor", UUID.randomUUID().toString())
            .param("limit", "20"))
        .andExpect(status().isOk());

    // 조회 서비스 호출 검증
    verify(notificationService, times(1)).getNotifications(any(UUID.class), any(String.class), any(), anyInt());
  }

  @Test
  @DisplayName("날짜(after) 조건만 있을 때 알림 목록을 조회한다.")
  void getNotifications_WithAfterOnly() throws Exception {
    mockMvc.perform(get("/api/notifications")
            .header(USER_ID_HEADER, VALID_USER_ID)
            .param("after", "2024-04-24T15:00:00")
            .param("limit", "20"))
        .andExpect(status().isOk());

    // 조회 서비스 호출 검증
    verify(notificationService, times(1)).getNotifications(any(UUID.class), any(), any(), anyInt());
  }
}
