package com.example.monew.domain.article.service;

import com.example.monew.domain.article.dto.ArticleDto;
import com.example.monew.domain.article.dto.ArticleRestoreResultDto;
import com.example.monew.domain.article.dto.CursorPageResponseArticleDto;
import com.example.monew.domain.article.entity.ArticleViewEntity;
import com.example.monew.domain.article.exception.ArticleNotFoundException;
import com.example.monew.domain.notification.event.ArticleRegisteredEvent;
import com.example.monew.domain.article.mapper.ArticleMapper;
import com.example.monew.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.article.repository.ArticleRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArticleService {
  private final ArticleRepository articleRepository;
  private final ArticleMapper articleMapper;
  private final ArticleViewService articleViewService;
  private final ApplicationEventPublisher eventPublisher;
  private final jakarta.persistence.EntityManager entityManager; // 추가

  @Transactional
  public ArticleDto getArticleDetail(UUID id) {
    log.info("뉴스 상세 조회 요청 - ID: {}", id);
    ArticleEntity article = articleRepository.findById(id)
        .filter(a -> !a.isDeleted())
        .orElseThrow(() -> {
          log.warn("뉴스 조회 실패 - 존재하지 않거나 삭제된 ID: {}", id);
          return new ArticleNotFoundException(ErrorCode.ARTICLE_NOT_FOUND);
        });

    return articleMapper.toDto(article, false);
  }

  @Transactional
  public void saveArticle(ArticleEntity article) {
    if (!articleRepository.existsBySourceUrl(article.getSourceUrl())) {
      articleRepository.save(article);
      log.debug("개별 뉴스 저장 완료 - URL: {}", article.getSourceUrl());

      if (article.getInterest() != null && !article.getInterest().isBlank()) {
        eventPublisher.publishEvent(new ArticleRegisteredEvent(
            article.getId(),
            article.getTitle(),
            article.getInterest()
        ));
      }
    } else {
      log.debug("중복 뉴스 스킵 - URL: {}", article.getSourceUrl());
    }
  }

  @Transactional
  public void isDeleted(UUID id) {
    log.info("뉴스 논리 삭제 요청 - ID: {}", id);

    ArticleEntity article = articleRepository.findById(id)
        .filter(a -> !a.isDeleted())
        .orElseThrow(() -> {
          log.warn("[DeleteArticle] 삭제 실패 - 존재하지 않거나 이미 삭제된 ID: {}", id);
          return new ArticleNotFoundException(ErrorCode.ARTICLE_NOT_FOUND);
        });

    articleRepository.softDelete(id);
    entityManager.flush();
    entityManager.clear();


    log.info("뉴스 논리 삭제 완료 - ID: {}", id);
  }

  @Transactional
  public void hardDelete(UUID id) {
    log.info("뉴스 물리 삭제 진행 - ID: {}", id);
    articleRepository.hardDeleteById(id);
  }
  @Transactional
  public ArticleRestoreResultDto restore(UUID id) {
    log.info("뉴스 복구 요청 - ID: {}", id);
    articleRepository.restoreById(id);

    entityManager.flush();
    entityManager.clear();

    return new ArticleRestoreResultDto(
        LocalDateTime.now(),
        List.of(id),
        1L
    );
  }

  public List<String> getAllSources() {
    return articleRepository.findAllSources();
  }

  @Transactional
  public void incrementViewCount(UUID articleId, UUID viewedBy, String clientIp) {
    log.info("조회수 로그 기록 - Article: {}, User: {}, IP: {}", articleId, viewedBy, clientIp);
    articleViewService.logView(articleId, viewedBy, clientIp);
  }
  public CursorPageResponseArticleDto getArticles(UUID cursor, LocalDateTime after, int size) {
    log.info("목록 조회 요청 - Cursor: {}, After: {}, Size: {}", cursor, after, size);

    List<ArticleEntity> articles =
        articleRepository.findByCursor(cursor, after, size);

    boolean hasNext = articles.size() > size;

    if (hasNext) {
      articles.remove(size);
    }
    log.debug("DB 조회 완료 - 결과 건수: {}, 다음 페이지 존재 여부: {}", articles.size(), hasNext);

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

  @Transactional
  public void saveInChunks(List<ArticleEntity> articles) {
    if (articles.isEmpty()) return;

    List<String> sourceUrls = articles.stream()
        .map(ArticleEntity::getSourceUrl)
        .toList();
    Set<String> existingUrls = articleRepository.findAllBySourceUrlIn(sourceUrls)
        .stream()
        .map(ArticleEntity::getSourceUrl)
        .collect(Collectors.toSet());

    List<ArticleEntity> newArticles = articles.stream()
        .filter(article -> !existingUrls.contains(article.getSourceUrl()))
        .toList();

    if (!newArticles.isEmpty()) {
      articleRepository.saveAll(newArticles);
      log.info("{}건 신규 뉴스 저장 완료", newArticles.size());
    }
  }
}