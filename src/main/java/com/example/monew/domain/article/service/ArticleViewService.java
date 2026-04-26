package com.example.monew.domain.article.service;

import com.example.monew.domain.article.dto.ArticleViewDto;
import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.article.entity.ArticleViewEntity;
import com.example.monew.domain.article.exception.ArticleNotFoundException;
import com.example.monew.domain.article.mapper.ArticleMapper;
import com.example.monew.domain.article.repository.ArticleRepository;
import com.example.monew.domain.article.repository.ArticleViewRepository;
import com.example.monew.global.exception.ErrorCode;
import jakarta.transaction.Transactional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ArticleViewService {

  private final ArticleRepository articleRepository;
  private final ArticleViewRepository articleViewRepository;

  @Transactional
  public ArticleViewDto logView(UUID articleId, UUID viewedBy, String clientIp) {
    ArticleEntity article = articleRepository.findById(articleId)
        .orElseThrow(() -> new ArticleNotFoundException(ErrorCode.ARTICLE_NOT_FOUND));

    boolean alreadyViewed = articleViewRepository.existsByArticleAndViewedBy(article, viewedBy);

    if (alreadyViewed) {
      // 이미 본 경우: DB 저장 없이 기존 정보로 DTO 생성 (ID 등 로그 정보는 null 처리)
      return buildDto(article, null);
    }

    article.incrementViewCount();

    ArticleViewEntity viewRecord = ArticleViewEntity.builder()
        .article(article)
        .viewedBy(viewedBy)
        .clientIp(clientIp)
        .build();

    ArticleViewEntity saved = articleViewRepository.save(viewRecord);
    return buildDto(article, saved);
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
