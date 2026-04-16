package com.example.monew.domain.article.repository;

import com.example.monew.domain.article.entity.ArticleEntity;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class ArticleRepositoryImpl implements ArticleRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  QArticleEntity article = QArticleEntity.articleEntity;

  @Override
  public List<ArticleEntity> findByCursor(Long cursor, LocalDateTime after, int size) {

    return queryFactory
        .selectFrom(article)
        .where(
            ltCursor(cursor),
            ltAfter(after)
        )
        .orderBy(article.createdAt.desc(), article.id.desc())
        .limit(size + 1) // 다음 페이지 확인용
        .fetch();
  }

  private BooleanExpression ltCursor(Long cursor) {
    return cursor != null ? article.id.lt(cursor) : null;
  }

  private BooleanExpression ltAfter(LocalDateTime after) {
    return after != null ? article.createdAt.lt(after) : null;
  }
}