package com.example.monew.domain.article.controller;

import com.example.monew.domain.article.dto.ArticleDto;
import com.example.monew.domain.article.dto.CursorPageResponseArticleDto;
import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.article.service.ArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Tag(name = "article", description = "뉴스 기사 API")
@RestController
@RequestMapping("/api/articles")

@RequiredArgsConstructor
public class ArticleController {

  private final ArticleService articleService;


  @Operation(summary = "뉴스 기사 목록 조회", description = "뉴스 기사 목록 조회")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "목록 조회 성공")
  })
  @GetMapping
  public ResponseEntity<CursorPageResponseArticleDto> getArticleList(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String interest,
      @RequestParam(required = false) String source,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
      @PageableDefault(size = 10, sort = "publishDate", direction = Sort.Direction.DESC) Pageable pageable
  ) {
    return ResponseEntity.ok(articleService.getArticleList(keyword, interest, source, startDate, endDate,pageable));
  }

  @Operation(summary = "뉴스 기사 상세 조회", description = "ID를 사용하여 뉴스 기사의 상세 내용을 조회합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "상세 조회 성공"),
      @ApiResponse(responseCode = "404", description = "기사를 찾을 수 없음")
  })
  @GetMapping("/{articleId}")
  public ResponseEntity<ArticleDto> getArticleDetail(@PathVariable UUID articleId) {
    return ResponseEntity.ok(articleService.getArticleDetail(articleId));
  }

  @Operation(summary = "기사 뷰 등록", description = "특정 기사 조회했을 때 조회 기록을 남기고 조회수 증가")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "뷰 등록 성공"),
      @ApiResponse(responseCode = "404", description = "기사를 찾을 수 없음")
  })


  @PostMapping("/{articleId}/article-views")
  public ResponseEntity<Void> incrementArticleView(@PathVariable UUID articleId) {
    articleService.incrementViewCount(articleId);
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "뉴스 기사 논리 삭제", description = "기사를 논리 삭제")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "삭제 성공"),
      @ApiResponse(responseCode = "404", description = "기사를 찾을 수 없음")
  })
  @DeleteMapping("/{articleId}")
  public ResponseEntity<Void> isDeletedArticle(@PathVariable UUID articleId) {
    articleService.isDeleted(articleId);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "뉴스 기사 물리 삭제", description = "기사 데이터베이스에서 영구적으로 삭제")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "영구 삭제 성공")
  })
  @DeleteMapping("/{articleId}/hard")
  public ResponseEntity<Void> hardDeleteArticle(@PathVariable UUID articleId) {
    articleService.hardDelete(articleId);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "뉴스 복구", description = "논리 삭제된 뉴스 기사를 다시 복구")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "복구 성공")
  })
  @GetMapping("/restore")
  public ResponseEntity<Void> restoreArticle(@RequestParam UUID articleId) {
    articleService.restore(articleId);
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "출처 목록 조회", description = "현재 등록된 모든 뉴스 기사의 출처 목록 조회")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "출처 조회 성공")
  })
  @GetMapping("/sources")
  public ResponseEntity<List<String>> getArticleSources() {
    return ResponseEntity.ok(articleService.getAllSources());
  }
}
