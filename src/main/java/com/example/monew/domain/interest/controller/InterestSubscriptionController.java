package com.example.monew.domain.interest.controller;

import com.example.monew.domain.interest.dto.SubscriptionResponse;
import com.example.monew.domain.interest.service.InterestSubscriptionService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
public class InterestSubscriptionController {

  private static final String USER_HEADER = "Monew-Request-User-ID";

  private final InterestSubscriptionService service;

  @PostMapping
  public ResponseEntity<SubscriptionResponse> subscribe(
      @PathVariable UUID interestId,
      @RequestHeader(USER_HEADER) UUID userId) {
    return ResponseEntity.ok(service.subscribe(interestId, userId));
  }

  @DeleteMapping
  public ResponseEntity<Void> unsubscribe(
      @PathVariable UUID interestId,
      @RequestHeader(USER_HEADER) UUID userId) {
    service.unsubscribe(interestId, userId);
    return ResponseEntity.ok().build();
  }
}
