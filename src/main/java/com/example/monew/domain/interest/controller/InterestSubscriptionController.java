package com.example.monew.domain.interest.controller;

import com.example.monew.domain.interest.dto.SubscriptionResponse;
import com.example.monew.domain.interest.service.InterestSubscriptionService;
import com.example.monew.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관심사 관리", description = "관심사 관련 API")
@RestController
@RequestMapping("/api/interests/{interestId}/subscriptions")
@RequiredArgsConstructor
public class InterestSubscriptionController {

  private static final String USER_HEADER = "Monew-Request-User-ID";

  private final InterestSubscriptionService interestSubscriptionService;

  @Operation(summary = "관심사 구독",
      description = "요청자(Monew-Request-User-ID)가 해당 관심사를 구독합니다. 중복 구독은 409를 반환합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "구독 성공"),
      @ApiResponse(responseCode = "404", description = "관심사 또는 사용자 정보 없음 (INTEREST_NOT_FOUND / USER_NOT_FOUND)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "409", description = "이미 구독 중 (DUPLICATE_SUBSCRIPTION)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PostMapping
  public ResponseEntity<SubscriptionResponse> subscribe(
      @PathVariable UUID interestId,
      @RequestHeader(USER_HEADER) UUID userId) {
    return ResponseEntity.ok(interestSubscriptionService.subscribe(interestId, userId));
  }

  @Operation(summary = "관심사 구독 취소",
      description = "요청자(Monew-Request-User-ID)의 구독을 취소합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "구독 취소 성공"),
      @ApiResponse(responseCode = "404", description = "구독 정보 없음 (SUBSCRIPTION_NOT_FOUND)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @DeleteMapping
  public ResponseEntity<Void> unsubscribe(
      @PathVariable UUID interestId,
      @RequestHeader(USER_HEADER) UUID userId) {
    interestSubscriptionService.unsubscribe(interestId, userId);
    return ResponseEntity.ok().build();
  }
}
