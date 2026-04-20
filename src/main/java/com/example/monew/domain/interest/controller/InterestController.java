package com.example.monew.domain.interest.controller;

import com.example.monew.domain.interest.dto.CursorPageResponse;
import com.example.monew.domain.interest.dto.InterestCreateRequest;
import com.example.monew.domain.interest.dto.InterestResponse;
import com.example.monew.domain.interest.dto.InterestUpdateRequest;
import com.example.monew.domain.interest.service.InterestService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interests")
@RequiredArgsConstructor
public class InterestController {

  private final InterestService interestService;

  @GetMapping
  public ResponseEntity<CursorPageResponse<InterestResponse>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam String orderBy,
      @RequestParam String direction,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime after,
      @RequestParam int limit,
      @RequestHeader("MoNew-Request-User-ID") UUID userId) {
    return ResponseEntity.ok(
        interestService.getInterests(keyword, orderBy, direction, cursor, after, limit, userId));
  }

  @PostMapping
  public ResponseEntity<InterestResponse> create(
      @RequestHeader("MoNew-Request-User-ID") UUID userId,
      @Valid @RequestBody InterestCreateRequest request) {
    InterestResponse response = interestService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PatchMapping("/{id}")
  public ResponseEntity<InterestResponse> updateKeywords(
      @RequestHeader("MoNew-Request-User-ID") UUID userId,
      @PathVariable UUID id,
      @Valid @RequestBody InterestUpdateRequest request) {
    return ResponseEntity.ok(interestService.updateKeywords(id, request));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(
      @RequestHeader("MoNew-Request-User-ID") UUID userId,
      @PathVariable UUID id) {
    interestService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
