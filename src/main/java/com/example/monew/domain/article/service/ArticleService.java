package com.example.monew.domain.article.service;

import com.example.monew.domain.article.dto.ArticleDto;
import com.example.monew.domain.article.dto.ArticleRestoreResultDto;
import com.example.monew.domain.article.dto.CursorPageResponseArticleDto;
import com.example.monew.domain.article.exception.ArticleNotFoundException;
import com.example.monew.domain.article.mapper.ArticleMapper;
import com.example.monew.global.exception.ErrorCode;
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

  public CursorPageResponseArticleDto getArticles(UUID cursor, LocalDateTime after, int size) {

    List<ArticleEntity> articles =
        articleRepository.findByCursor(cursor, after, size);

    boolean hasNext = articles.size() > size;

    if (hasNext) {
      articles.remove(size);
    }

    List<ArticleDto> content = articles.stream()
        .map(a -> articleMapper.toDto(a, false))
        .toList();

    UUID nextCursor = null;
    LocalDateTime nextAfter = null;

    if (!articles.isEmpty()) {
      ArticleEntity last = articles.get(articles.size() - 1);
      nextCursor = last.getId();
      nextAfter = last.getCreatedAt();
    }

    return new CursorPageResponseArticleDto(
        content,
        nextCursor != null ? nextCursor.toString() : null,
        nextAfter,
        size,
        null,
        hasNext
    );
  }
}