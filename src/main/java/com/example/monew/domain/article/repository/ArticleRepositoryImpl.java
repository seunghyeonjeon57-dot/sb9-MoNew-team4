package com.example.monew.domain.article.repository;

import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.article.entity.QArticleEntity;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class ArticleRepositoryImpl implements ArticleRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  QArticleEntity article = QArticleEntity.articleEntity;

  @Override
  public List<ArticleEntity> findByCursor(UUID cursor, LocalDateTime after, int size) {

    return queryFactory
        .selectFrom(article)
        .where(
            ltCursor(cursor),
            ltAfter(after)
        )
        .orderBy(article.createdAt.desc(), article.id.desc())
        .limit(size + 1)
        .fetch();
  }

  private BooleanExpression ltCursor(UUID cursor) {
    return cursor != null ? article.id.lt(cursor) : null;
  }

  private BooleanExpression ltAfter(LocalDateTime after) {
    return after != null ? article.createdAt.lt(after) : null;
  }
}