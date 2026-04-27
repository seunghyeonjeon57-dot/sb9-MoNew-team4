package com.example.monew.domain.interest.controller;

import com.example.monew.domain.interest.dto.CursorPageResponse;
import com.example.monew.domain.interest.dto.InterestCreateRequest;
import com.example.monew.domain.interest.dto.InterestResponse;
import com.example.monew.domain.interest.dto.InterestUpdateRequest;
import com.example.monew.domain.interest.service.InterestService;
import com.example.monew.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "관심사 관리", description = "관심사 관련 API")
@RestController
@RequestMapping("/api/interests")
@RequiredArgsConstructor
public class InterestController {

  private final InterestService interestService;

  @Operation(summary = "관심사 목록 조회", description = "커서 기반 페이지네이션으로 관심사 목록을 조회합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 정렬/헤더 파라미터",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @GetMapping
  public ResponseEntity<CursorPageResponse<InterestResponse>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam String orderBy,
      @RequestParam String direction,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime after,
      @RequestParam int limit,
      @RequestHeader("Monew-Request-User-ID") UUID userId) {
    return ResponseEntity.ok(
        interestService.getInterests(keyword, orderBy, direction, cursor, after, limit, userId));
  }

  @Operation(summary = "관심사 등록", description = "새 관심사를 등록합니다. 기존 이름과 80% 이상 유사한 경우 409를 반환합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "등록 성공"),
      @ApiResponse(responseCode = "400", description = "입력값 검증 실패",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "409", description = "유사 관심사 이름 중복 (SIMILAR_INTEREST_NAME)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PostMapping
  public ResponseEntity<InterestResponse> create(
      @Valid @RequestBody InterestCreateRequest request) {
    InterestResponse response = interestService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Operation(summary = "관심사 키워드 수정", description = "관심사의 키워드 목록만 수정합니다. 이름 필드가 포함되면 400을 반환합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "수정 성공"),
      @ApiResponse(responseCode = "400", description = "입력값 검증 실패 또는 이름 수정 시도 (INTEREST_NAME_IMMUTABLE)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "관심사 정보 없음 (INTEREST_NOT_FOUND)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PatchMapping("/{interestId}")
  public ResponseEntity<InterestResponse> updateKeywords(
      @PathVariable UUID interestId,
      @Valid @RequestBody InterestUpdateRequest request) {
    return ResponseEntity.ok(interestService.updateKeywords(interestId, request, null));
  }

  @Operation(summary = "관심사 물리 삭제", description = "관심사를 물리 삭제합니다. 구독 레코드도 함께 제거됩니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "삭제 성공"),
      @ApiResponse(responseCode = "404", description = "관심사 정보 없음 (INTEREST_NOT_FOUND)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @DeleteMapping("/{interestId}")
  public ResponseEntity<Void> delete(
      @PathVariable UUID interestId) {
    interestService.delete(interestId);
    return ResponseEntity.noContent().build();
  }
}
