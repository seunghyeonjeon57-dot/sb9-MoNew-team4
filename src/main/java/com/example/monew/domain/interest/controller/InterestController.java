package com.example.monew.domain.interest.controller;

import com.example.monew.domain.interest.dto.CursorSlice;
import com.example.monew.domain.interest.dto.InterestCreateRequest;
import com.example.monew.domain.interest.dto.InterestResponse;
import com.example.monew.domain.interest.dto.InterestUpdateRequest;
import com.example.monew.domain.interest.service.InterestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
@Tag(name = "Interest", description = "관심사 도메인 API")
public class InterestController {

  private final InterestService interestService;

  @PostMapping
  @Operation(summary = "관심사 등록", description = "유사도 80% 이상 이름 등록은 차단")
  public ResponseEntity<InterestResponse> create(@Valid @RequestBody InterestCreateRequest request) {
    InterestResponse response = interestService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping
  @Operation(summary = "관심사 목록 조회 (검색 + 정렬 + 커서 페이지네이션)")
  public ResponseEntity<CursorSlice<InterestResponse>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false, defaultValue = "name") String sortBy,
      @RequestParam(required = false, defaultValue = "asc") String direction,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false, defaultValue = "20") int size,
      @RequestHeader(value = "MoNew-Request-User-ID", required = false) UUID userId) {
    return ResponseEntity.ok(
        interestService.getInterests(keyword, sortBy, direction, cursor, size, userId));
  }

  @PatchMapping("/{interestId}")
  @Operation(summary = "관심사 키워드 수정", description = "이름은 불변, 키워드만 교체")
  public ResponseEntity<InterestResponse> updateKeywords(
      @PathVariable UUID interestId,
      @Valid @RequestBody InterestUpdateRequest request) {
    return ResponseEntity.ok(interestService.updateKeywords(interestId, request));
  }

  @DeleteMapping("/{interestId}")
  @Operation(summary = "관심사 삭제 (soft delete + 구독 cascade)")
  public ResponseEntity<Void> delete(@PathVariable UUID interestId) {
    interestService.delete(interestId);
    return ResponseEntity.noContent().build();
  }
}
