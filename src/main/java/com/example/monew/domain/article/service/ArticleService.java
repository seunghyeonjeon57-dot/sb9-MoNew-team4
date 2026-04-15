package com.example.monew.domain.article.service;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.article.repository.ArticleRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArticleService {

  private final ArticleRepository articleRepository;

  @Transactional
  public ArticleEntity getArticleDetail(UUID id) {
    ArticleEntity article = articleRepository.findById(id)
          // 예외처리 임시 -> 글로벌에 추가
        .orElseThrow(() -> new IllegalArgumentException("해당 기사를 찾을 수 없습니다."));

    article.incrementViewCount();

    return article;
  }

  @Transactional
  public void saveArticle(ArticleEntity article) {
    if (!articleRepository.existsBySourceUrl(article.getSourceUrl())) {
      articleRepository.save(article);
    }
  }

  @Transactional
  public void isDeleted(UUID id) {
    ArticleEntity article = articleRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("해당 기사를 찾을 수 없습니다."));

    articleRepository.delete(article);
  }
  @Transactional
  public void hardDelete(UUID id) {
    articleRepository.hardDeleteById(id);
  }

  @Transactional
  public void restore(UUID id) {
    articleRepository.restoreById(id);
  }

  public List<String> getAllSources() {
    return articleRepository.findAllSources();
  }

  //임시
  public void incrementViewCount(UUID articleId) {
  }


  public Page<ArticleEntity> getArticleList(String keyword, String interest, String source,
      LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
    // 모든 조건을 레포지토리에 던지면, 쿼리에서 null 체크를 통해 알아서 합쳐서 검색합니다.
    return articleRepository.searchArticles(keyword, interest, source, startDate, endDate, pageable);
  }

}