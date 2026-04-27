package com.example.monew.domain.article.service;

import com.example.monew.domain.article.dto.ArticleDto;
import com.example.monew.domain.article.dto.ArticleRestoreResultDto;
import com.example.monew.domain.article.dto.ArticleSearchCondition;
import com.example.monew.domain.article.dto.CursorPageResponseArticleDto;
import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.article.exception.ArticleNotFoundException;
import com.example.monew.domain.article.mapper.ArticleMapper;
import com.example.monew.domain.article.repository.ArticleRepository;
import com.example.monew.domain.notification.event.ArticleRegisteredEvent;
import com.example.monew.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArticleService {

  private final ArticleRepository articleRepository;
  private final ArticleMapper articleMapper;
  private final ArticleViewService articleViewService;
  private final ApplicationEventPublisher eventPublisher;
  private final EntityManager entityManager;

  @Transactional
  public ArticleDto getArticleDetail(UUID id) {
    log.info("뉴스 상세 조회 요청 - ID: {}", id);
    ArticleEntity article = articleRepository.findById(id)
        .filter(a -> !a.isDeleted())
        .orElseThrow(() -> {
          log.warn("뉴스 조회 실패 - 존재하지 않거나 삭제된 ID: {}", id);
          return new ArticleNotFoundException(ErrorCode.ARTICLE_NOT_FOUND);
        });
    if (article.getInterest() != null) {
      article.getInterest().getKeywords().size();
      log.debug("관심사 키워드 {}건 로딩 완료", article.getInterest().getKeywords().size());
    }
    return articleMapper.toDto(article, false);
  }

  @Transactional
  public void saveArticle(ArticleEntity article) {
    if (!articleRepository.existsBySourceUrl(article.getSourceUrl())) {
      articleRepository.save(article);
      log.debug("개별 뉴스 저장 완료 - URL: {}", article.getSourceUrl());

      if (article.getInterest() != null &&
          article.getInterest().getName() != null &&
          !article.getInterest().getName().isBlank()) {

        eventPublisher.publishEvent(new ArticleRegisteredEvent(
            article.getId(),
            article.getTitle(),
            article.getInterest().getName()
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

  public CursorPageResponseArticleDto getArticles(ArticleSearchCondition condition) {
    log.info("목록 조회 요청 - Condition: {}", condition);

    List<ArticleEntity> articles = articleRepository.findByCursor(condition);

    int size = condition.getSize();
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
    if (articles == null || articles.isEmpty()) return;

    List<String> sourceUrls = articles.stream()
        .map(ArticleEntity::getSourceUrl)
        .toList();

    Set<String> existingUrls = articleRepository.findAllBySourceUrlIn(sourceUrls)
        .stream()
        .map(ArticleEntity::getSourceUrl)
        .collect(Collectors.toSet());

    List<ArticleEntity> newArticles = articles.stream()
        .filter(a -> !existingUrls.contains(a.getSourceUrl()))
        .toList();

    if (!newArticles.isEmpty()) {
      articleRepository.saveAll(newArticles);

      log.info("신규 뉴스 {}건 일괄 저장 완료", newArticles.size());

      newArticles.forEach(article -> {
        if (article.getInterest() != null &&
            article.getInterest().getName() != null &&
            !article.getInterest().getName().isBlank()){

          eventPublisher.publishEvent(new ArticleRegisteredEvent(
              article.getId(),
              article.getTitle(),
              article.getInterest().getName()
          ));
        }
      });
    }
  }

  public List<String> getAllSources() {
    return articleRepository.findAllSources();
  }

  @Transactional
  public void incrementViewCount(UUID articleId, UUID viewedBy, String clientIp) {
    log.info("조회수 로그 기록 요청 - Article: {}, User: {}", articleId, viewedBy);

    articleViewService.logView(articleId, viewedBy, clientIp);

  }

}
