package com.example.monew.domain.interest.controller;

import com.example.monew.domain.interest.dto.SubscriptionDto;
import com.example.monew.domain.interest.service.InterestSubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
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
  @Operation(summary = "관심사 구독", description = "헤더 MoNew-Request-User-ID 기반")
  public ResponseEntity<SubscriptionDto> subscribe(
      @PathVariable UUID interestId,
      @RequestHeader("MoNew-Request-User-ID") UUID userId) {
    SubscriptionDto dto = subscriptionService.subscribe(interestId, userId);
    return ResponseEntity.status(HttpStatus.CREATED).body(dto);
  }

  @DeleteMapping
  @Operation(summary = "관심사 구독 취소")
  public ResponseEntity<Void> unsubscribe(
      @PathVariable UUID interestId,
      @RequestHeader("MoNew-Request-User-ID") UUID userId) {
    subscriptionService.unsubscribe(interestId, userId);
    return ResponseEntity.noContent().build();
  }
}
