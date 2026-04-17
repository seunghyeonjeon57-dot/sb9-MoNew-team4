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

  @GetMapping
  @Operation(summary = "알림 목록 조회", description = "알림 목록을 커서 기반 페이징으로 조회합니다.")
  public ResponseEntity<CursorPageResponseNotificationDto> getNotifications(
      @Parameter(description = "요청자 ID", required = true)
      @RequestHeader("Monew-Request-User-ID") UUID userId,

      @Parameter(description = "커서 값(ID)")
      @RequestParam(required = false) String cursor,

      @Parameter(description = "보조 커서(생성일자)")
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime after,

      @Parameter(description = "페이지 크기", example = "50")
      @RequestParam(defaultValue = "50") int limit
  ) {
    return ResponseEntity.ok(notificationService.getNotifications(userId, cursor, after, limit));
  }

  @PatchMapping("/{notificationId}")
  @Operation(summary = "알림 확인", description = "특정 알림을 확인 처리합니다.")
  public ResponseEntity<Void> confirmNotification(
      @Parameter(description = "알림 ID", required = true)
      @PathVariable UUID notificationId,

      @Parameter(description = "요청자 ID", required = true)
      @RequestHeader("Monew-Request-User-ID") UUID userId
  ) {
    notificationService.readNotification(notificationId);
    return ResponseEntity.ok().build();
  }

  @PatchMapping
  @Operation(summary = "전체 알림 확인", description = "사용자의 모든 알림을 한번에 확인 처리합니다.")
  public ResponseEntity<Void> confirmAllNotifications(
      @Parameter(description = "요청자 ID", required = true)
      @RequestHeader("Monew-Request-User-ID") UUID userId
  ) {
    // TODO: 서비스에 전체 읽음 처리 로직(예: updateAllByUserId)을 추가한 뒤 호출하세요.
    // notificationService.confirmAllNotifications(userId);
    return ResponseEntity.ok().build();
  }
}