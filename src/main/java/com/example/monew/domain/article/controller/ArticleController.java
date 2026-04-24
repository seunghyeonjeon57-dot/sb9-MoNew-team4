package com.example.monew.domain.article.controller;

import com.example.monew.domain.article.batch.BackupBatch;
import com.example.monew.domain.article.batch.service.BackupService;
import com.example.monew.domain.article.dto.ArticleDto;
import com.example.monew.domain.article.dto.ArticleSearchCondition;
import com.example.monew.domain.article.dto.CursorPageResponseArticleDto;
import com.example.monew.domain.article.exception.InvalidCursorException;
import com.example.monew.domain.article.exception.InvalidRestoreDateException;
import com.example.monew.domain.article.service.ArticleService;
import com.example.monew.domain.article.service.ArticleViewService;
import com.example.monew.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Tag(name = "뉴스 기사 관리", description = "뉴스 기사 관련API")
@RestController
@RequestMapping("/api/articles")

@RequiredArgsConstructor
public class ArticleController {

  private final ArticleService articleService;
  private final ArticleViewService articleViewService;
  private final BackupService backupService;
  private final BackupBatch backupBatch;

  @Operation(summary = "뉴스 기사 목록 조회", description = "조건에 맞는 뉴스 기사 목록을 조회합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 (정렬 기준 오류, 페이지네이션 파라미터 오류 등)"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })

  @GetMapping
  public ResponseEntity<CursorPageResponseArticleDto> getArticleList(
      @ModelAttribute ArticleSearchCondition condition
  ) {
    if (condition.getSize() > 100) {
      throw new InvalidCursorException(ErrorCode.INVALID_INPUT_VALUE);
    }
    return ResponseEntity.ok(articleService.getArticles(condition));
  }

  @Operation(summary = "뉴스 기사 단건 조회", description = "뉴스 기사 ID로 뉴스 기사 단건을 조회합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "404", description = "뉴스 기사 정보 없음"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")

  })

  @GetMapping("/{articleId}")
  public ResponseEntity<ArticleDto> getArticleDetail(
      @PathVariable UUID articleId
  ) {
    return ResponseEntity.ok(articleService.getArticleDetail(articleId));
  }

  @Operation(summary = "기사 뷰 등록", description = "기사 뷰를 등록합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "기사 뷰 등록 성공"),
      @ApiResponse(responseCode = "404", description = "기사 정보 없음")
  })

  @PostMapping("/{articleId}/article-views")
  public ResponseEntity<Void> incrementArticleView(
      @PathVariable UUID articleId,
      HttpServletRequest request
  ) {
    String clientIp = request.getRemoteAddr();
    UUID viewedBy = UUID.randomUUID();
    articleViewService.logView(articleId, viewedBy, clientIp);
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "뉴스 기사 논리 삭제", description = "뉴스 기사를 논리적으로 삭제합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "논리 삭제 성공"),
      @ApiResponse(responseCode = "404", description = "기사를 찾을 수 없음"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")

  })
  @DeleteMapping("/{articleId}")
  public ResponseEntity<Void> isDeletedArticle(@PathVariable UUID articleId) {
    articleService.isDeleted(articleId);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "뉴스 기사 물리 삭제", description = "뉴스 기사를 물리적으로 삭제합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "삭제 성공"),
      @ApiResponse(responseCode = "404", description = "뉴스 기사 정보 없음"),
      @ApiResponse(responseCode = "204", description = "서버 내부 오류")
  })
  @DeleteMapping("/{articleId}/hard")
  public ResponseEntity<Void> hardDeleteArticle(@PathVariable UUID articleId) {
    articleService.hardDelete(articleId);
    return ResponseEntity.noContent().build();
  }


  @Operation(summary = "뉴스 복구", description = "유실된 뉴스 기사를 복구.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "복구 성공"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  @PostMapping("/restore")
  public ResponseEntity<String> restoreFromS3(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate date
  ) {
    if (date.isAfter(java.time.LocalDate.now())) {
      throw new InvalidRestoreDateException(ErrorCode.INVALID_INPUT_VALUE);
    }

    backupService.restoreNews(date);
    return ResponseEntity.ok(date + " 자 데이터 S3 복구 프로세스 완료");
  }


  @Operation(summary = "출처 목록 조회", description = "출처 목록을 조회합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "출처 조회 성공"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  @GetMapping("/sources")
  public ResponseEntity<List<String>> getArticleSources() {
    return ResponseEntity.ok(articleService.getAllSources());
  }
}
