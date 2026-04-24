package com.example.monew.domain.article.service;

import com.example.monew.domain.activity.service.ActivityService;
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
  private final ActivityService activityService;
  @Transactional
  public ArticleViewDto logView(UUID articleId, UUID viewedBy, String clientIp) {
    ArticleEntity article = articleRepository.findById(articleId)
        .orElseThrow(() -> new ArticleNotFoundException(ErrorCode.ARTICLE_NOT_FOUND));

    article.incrementViewCount(); // 기사 엔티티에

    ArticleViewEntity viewRecord = ArticleViewEntity.builder()
        .article(article)
        .viewedBy(viewedBy)
        .clientIp(clientIp)
        .build();

    ArticleViewEntity saved = articleViewRepository.save(viewRecord);

    ArticleViewDto responseDto = ArticleViewDto.builder()
        .id(saved.getId())
        .viewedBy(saved.getViewedBy())
        .createdAt(saved.getViewedAt())
        .articleId(article.getId())
        .source(article.getSource())
        .sourceUrl(article.getSourceUrl())
        .articleTitle(article.getTitle())
        .articlePublishedDate(article.getCreatedAt())
        .articleSummary(article.getSummary())
        .articleViewCount(article.getViewCount())
        .articleCommentCount(0L)
        .build();

    if (viewedBy != null) {
      activityService.updateRecentViewedArticles(viewedBy, responseDto);
    }

    return responseDto;
  }
}
