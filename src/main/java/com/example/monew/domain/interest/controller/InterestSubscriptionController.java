package com.example.monew.domain.interest.controller;

import com.example.monew.domain.interest.dto.SubscriptionDto;
import com.example.monew.domain.interest.service.InterestSubscriptionService;
import com.example.monew.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interests/{interestId}/subscriptions")
@RequiredArgsConstructor
@Tag(name = "InterestSubscription", description = "관심사 구독 API")
public class InterestSubscriptionController {

  private final InterestSubscriptionService subscriptionService;

  @PostMapping
  @Operation(summary = "관심사 구독",
      description = "MoNew-Request-User-ID 헤더의 사용자로 해당 관심사를 구독한다. 구독자 수가 +1 된다.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "구독 성공"),
      @ApiResponse(responseCode = "404", description = "관심사 없음",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "409", description = "이미 구독 중",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<SubscriptionDto> subscribe(
      @Parameter(description = "구독할 관심사 ID") @PathVariable UUID interestId,
      @Parameter(description = "요청자 ID") @RequestHeader("MoNew-Request-User-ID") UUID userId) {
    SubscriptionDto dto = subscriptionService.subscribe(interestId, userId);
    return ResponseEntity.status(HttpStatus.CREATED).body(dto);
  }

  @DeleteMapping
  @Operation(summary = "관심사 구독 취소",
      description = "MoNew-Request-User-ID 헤더의 사용자가 해당 관심사 구독을 취소한다. 구독자 수가 -1 된다.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "취소 성공"),
      @ApiResponse(responseCode = "400", description = "구독 중이 아닌 관심사",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "관심사 없음",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<Void> unsubscribe(
      @Parameter(description = "구독 취소할 관심사 ID") @PathVariable UUID interestId,
      @Parameter(description = "요청자 ID") @RequestHeader("MoNew-Request-User-ID") UUID userId) {
    subscriptionService.unsubscribe(interestId, userId);
    return ResponseEntity.noContent().build();
  }
}
