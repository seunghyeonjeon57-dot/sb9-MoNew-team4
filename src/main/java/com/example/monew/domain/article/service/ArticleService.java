package com.example.monew.domain.article.service;

import com.example.monew.domain.article.dto.ArticleDto;
import com.example.monew.domain.article.dto.ArticleRestoreResultDto;
import com.example.monew.domain.article.dto.CursorPageResponseArticleDto;
import com.example.monew.domain.article.exception.ArticleNotFoundException;
import com.example.monew.domain.article.mapper.ArticleMapper;
import com.example.monew.global.exception.ErrorCode;
import com.example.monew.global.exception.MonewException;
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
  private final ArticleMapper articleMapper;

  @Transactional
  public ArticleDto getArticleDetail(UUID id) {
    ArticleEntity article = articleRepository.findById(id)
        .orElseThrow(() -> new ArticleNotFoundException(ErrorCode.ARTICLE_NOT_FOUND));

    article.incrementViewCount();
    return articleMapper.toDto(article, false);
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
        .orElseThrow(() -> new ArticleNotFoundException(ErrorCode.ARTICLE_NOT_FOUND));

    articleRepository.delete(article);
  }

  @Transactional
  public void hardDelete(UUID id) {
    articleRepository.hardDeleteById(id);
  }

  public ArticleRestoreResultDto restore(UUID id) {
    articleRepository.restoreById(id);

    return new ArticleRestoreResultDto(
        LocalDateTime.now(),
        List.of(id),
        1L
    );
  }

  public List<String> getAllSources() {
    return articleRepository.findAllSources();
  }

  //임시
  public void incrementViewCount(UUID articleId) {
  }

  public CursorPageResponseArticleDto getArticleList(String keyword, String interest, String source, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
    Page<ArticleEntity> articlePage = articleRepository.searchArticles(keyword, interest, source, startDate, endDate, pageable);
    return articleMapper.toCursorPageDto(articlePage);
  }


}