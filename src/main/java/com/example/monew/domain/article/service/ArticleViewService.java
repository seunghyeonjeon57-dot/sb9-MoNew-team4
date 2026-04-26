package com.example.monew.domain.article.service;

import com.example.monew.domain.activity.service.ActivityService;
import com.example.monew.domain.article.dto.ArticleViewDto;
import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.article.entity.ArticleViewEntity;
import com.example.monew.domain.article.exception.ArticleNotFoundException;
import com.example.monew.domain.article.repository.ArticleRepository;
import com.example.monew.domain.article.repository.ArticleViewRepository;
import com.example.monew.global.exception.ErrorCode;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ArticleViewService {

  private final ArticleRepository articleRepository;
  private final ArticleViewRepository articleViewRepository;
  private final ActivityService activityService;

  @Transactional
  public ArticleViewDto logView(UUID articleId, UUID viewedBy, String clientIp) {
    ArticleEntity article = articleRepository.findById(articleId)
        .orElseThrow(() -> new ArticleNotFoundException(ErrorCode.ARTICLE_NOT_FOUND));

    // 1. 중복 조회 체크
    boolean alreadyViewed = articleViewRepository.existsByArticleEntityIdAndViewedBy(articleId, viewedBy);
    if (alreadyViewed) {
      return buildDto(article, null);
    }

    // 2. 조회수 증가 및 로그 저장
    article.incrementViewCount();
    ArticleViewEntity viewRecord = ArticleViewEntity.builder()
        .article(article)
        .viewedBy(viewedBy)
        .clientIp(clientIp)
        .build();

    ArticleViewEntity saved = articleViewRepository.save(viewRecord);

    // 3. 응답 DTO 생성
    ArticleViewDto responseDto = buildDto(article, saved);

    // 4. 최근 본 기사 업데이트
    if (viewedBy != null) {
      activityService.updateRecentViewedArticles(viewedBy, responseDto);
    }

    return responseDto;
  }

  private ArticleViewDto buildDto(ArticleEntity article, ArticleViewEntity saved) {
    return ArticleViewDto.builder()
        .id(saved != null ? saved.getId() : null)
        .viewedBy(saved != null ? saved.getViewedBy() : null)
        .createdAt(saved != null ? saved.getViewedAt() : null)
        .articleId(article.getId())
        .source(article.getSource())
        .sourceUrl(article.getSourceUrl())
        .articleTitle(article.getTitle())
        .articlePublishedDate(article.getCreatedAt())
        .articleSummary(article.getSummary())
        .articleViewCount(article.getViewCount())
        .articleCommentCount(0L)
        .build();
  }
}