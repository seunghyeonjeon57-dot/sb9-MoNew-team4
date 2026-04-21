package com.example.monew.domain.notification.controller;

import com.example.monew.domain.notification.dto.CursorPageResponseNotificationDto;
import com.example.monew.domain.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "알림 관리", description = "알림 관련 API")
public class NotificationController {

  private final NotificationService notificationService;

  @Operation(summary = "알림 목록 조회")
  @GetMapping
  public ResponseEntity<CursorPageResponseNotificationDto> getNotifications(
      @RequestHeader("Monew-Request-User-ID") UUID userId,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime after,
      @RequestParam(defaultValue = "50") int limit
  ) {
    return ResponseEntity.ok(notificationService.getNotifications(userId, cursor, after, limit));
  }

  @Operation(summary = "알림 확인", description = "특정 알림을 확인 처리합니다.")
  @PatchMapping("/{notificationId}")
  public ResponseEntity<Void> confirmNotification(
      @PathVariable UUID notificationId,
      @RequestHeader("Monew-Request-User-ID") UUID userId
  ) {
    // 수정: 서비스 호출 시 userId 추가 전달 (보안)
    notificationService.confirmNotification(notificationId, userId);
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "전체 알림 확인", description = "사용자의 모든 알림을 한번에 확인 처리합니다.")
  @PatchMapping
  public ResponseEntity<Void> confirmAllNotifications(
      @RequestHeader("Monew-Request-User-ID") UUID userId
  ) {
    // 수정: 주석 해제 및 서비스 호출
    notificationService.confirmAllNotifications(userId);
    return ResponseEntity.ok().build();
  }
}