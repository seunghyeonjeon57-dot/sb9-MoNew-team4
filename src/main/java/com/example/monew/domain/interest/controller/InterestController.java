package com.example.monew.domain.interest.controller;

import com.example.monew.domain.interest.dto.InterestCreateRequest;
import com.example.monew.domain.interest.dto.InterestResponse;
import com.example.monew.domain.interest.service.InterestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interests")
@RequiredArgsConstructor
@Tag(name = "Interest", description = "관심사 도메인 API")
public class InterestController {

  private final InterestService interestService;

  @PostMapping
  @Operation(summary = "관심사 등록", description = "유사도 80% 이상 이름 등록은 차단")
  public ResponseEntity<InterestResponse> create(@Valid @RequestBody InterestCreateRequest request) {
    InterestResponse response = interestService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }
}
