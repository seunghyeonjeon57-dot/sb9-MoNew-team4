package com.example.monew.domain.interest.controller;

import com.example.monew.domain.interest.dto.CursorSlice;
import com.example.monew.domain.interest.dto.InterestCreateRequest;
import com.example.monew.domain.interest.dto.InterestResponse;
import com.example.monew.domain.interest.dto.InterestUpdateRequest;
import com.example.monew.domain.interest.service.InterestService;
import com.example.monew.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
  @Operation(summary = "관심사 등록",
      description = "관심사를 생성한다. 기존 이름과 유사도 80% 이상이면 409로 거절된다.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "생성 성공"),
      @ApiResponse(responseCode = "400", description = "요청 검증 실패",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "409", description = "유사 이름 중복",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<InterestResponse> create(@Valid @RequestBody InterestCreateRequest request) {
    InterestResponse response = interestService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping
  @Operation(summary = "관심사 목록 조회",
      description = "이름/키워드 부분일치 검색, name 또는 subscriberCount 정렬, 커서 기반 페이지네이션을 지원한다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 sortBy/direction/cursor",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<CursorSlice<InterestResponse>> list(
      @Parameter(description = "이름 또는 키워드 부분일치 검색어") @RequestParam(required = false) String keyword,
      @Parameter(description = "정렬 기준 (name|subscriberCount)", example = "name")
      @RequestParam(required = false, defaultValue = "name") String sortBy,
      @Parameter(description = "정렬 방향 (asc|desc)", example = "asc")
      @RequestParam(required = false, defaultValue = "asc") String direction,
      @Parameter(description = "이전 페이지 응답의 nextCursor")
      @RequestParam(required = false) String cursor,
      @Parameter(description = "페이지 크기 (최대 100)", example = "20")
      @RequestParam(required = false, defaultValue = "20") int size,
      @Parameter(description = "요청자 ID. 지정 시 각 항목의 subscribed 플래그가 채워진다.")
      @RequestHeader(value = "MoNew-Request-User-ID", required = false) UUID userId) {
    return ResponseEntity.ok(
        interestService.getInterests(keyword, sortBy, direction, cursor, size, userId));
  }

  @PatchMapping("/{interestId}")
  @Operation(summary = "관심사 키워드 수정",
      description = "이름은 불변이며 키워드 리스트만 통째로 교체된다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "수정 성공"),
      @ApiResponse(responseCode = "400", description = "키워드 검증 실패",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "관심사 없음",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<InterestResponse> updateKeywords(
      @Parameter(description = "수정 대상 관심사 ID") @PathVariable UUID interestId,
      @Valid @RequestBody InterestUpdateRequest request) {
    return ResponseEntity.ok(interestService.updateKeywords(interestId, request));
  }

  @DeleteMapping("/{interestId}")
  @Operation(summary = "관심사 삭제",
      description = "soft delete 후 해당 관심사의 모든 구독을 cascade 삭제한다.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "삭제 성공"),
      @ApiResponse(responseCode = "404", description = "관심사 없음",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<Void> delete(
      @Parameter(description = "삭제 대상 관심사 ID") @PathVariable UUID interestId) {
    interestService.delete(interestId);
    return ResponseEntity.noContent().build();
  }
}
