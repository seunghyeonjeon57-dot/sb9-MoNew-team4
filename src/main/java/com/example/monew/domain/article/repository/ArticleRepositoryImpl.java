package com.example.monew.domain.article.repository;

import static com.example.monew.domain.article.entity.QArticleEntity.articleEntity;
import static com.querydsl.jpa.JPAExpressions.selectFrom;

import com.example.monew.domain.article.dto.ArticleSearchCondition;
import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.article.entity.QArticleEntity;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class ArticleRepositoryImpl implements ArticleRepositoryCustom {
  private final JPAQueryFactory queryFactory;

  private final QArticleEntity article = articleEntity;

  @Override
  public long softDelete(UUID articleId) {
    return queryFactory
        .update(article)
        .set(article.deletedAt, LocalDateTime.now())
        .where(article.id.eq(articleId))
        .execute();
  }

  @Override
  public List<ArticleEntity> findAllActive() {
    return queryFactory
        .selectFrom(article)
        .where(article.deletedAt.isNull())
        .fetch();
  }


  public ArticleRepositoryImpl(EntityManager em) {
    this.queryFactory = new JPAQueryFactory(em);
  }


  @Override
  public List<ArticleEntity> findByCursor(ArticleSearchCondition condition) {
    return queryFactory
        .selectFrom(article)
        .where(
            article.deletedAt.isNull(),
            keywordContains(condition.getKeyword()),
            sourceIn(condition.getSourceIn()),
            publishDateBetween(condition.getPublishDateFrom(), condition.getPublishDateTo()),
            cursorCondition(condition)
        )
        .orderBy(getOrderSpecifiers(condition))
        .limit(condition.getSize() + 1)
        .fetch();
  }

  private BooleanExpression keywordContains(String keyword) {
    if (keyword == null || keyword.isBlank()) return null;
    return article.title.containsIgnoreCase(keyword)
        .or(article.summary.containsIgnoreCase(keyword));
  }

  private BooleanExpression sourceIn(List<String> sources) {
    if (sources == null || sources.isEmpty()) return null;
    return article.source.in(sources);
  }

  private BooleanExpression publishDateBetween(LocalDateTime from, LocalDateTime to) {
    if (from == null && to == null) return null;
    if (from == null) return article.createdAt.loe(to);
    if (to == null) return article.createdAt.goe(from);
    return article.createdAt.between(from, to);
  }

  private OrderSpecifier<?> getOrderSpecifier(ArticleSearchCondition condition) {
    Order direction = condition.getDirection().equalsIgnoreCase("ASC") ? Order.ASC : Order.DESC;

    return switch (condition.getOrderBy()) {
      case "viewCount" -> new OrderSpecifier<>(direction, article.viewCount);
      case "commentCount" -> new OrderSpecifier<>(direction, article.id);
      default -> new OrderSpecifier<>(direction, article.createdAt);
    };
  }

  private BooleanExpression cursorCondition(ArticleSearchCondition condition) {
    if (condition.getCursor() == null) return null;

    ArticleEntity cursorArticle = queryFactory
        .selectFrom(article)
        .where(article.id.eq(condition.getCursor()))
        .fetchOne();

    if (cursorArticle == null) return null;

    String orderBy = condition.getOrderBy();
    Order direction = condition.getDirection().equalsIgnoreCase("ASC") ? Order.ASC : Order.DESC;

    if ("viewCount".equals(orderBy)) {
      long viewCount = cursorArticle.getViewCount();
      return direction == Order.DESC
          ? article.viewCount.lt(viewCount).or(article.viewCount.eq(viewCount).and(article.id.lt(condition.getCursor())))
          : article.viewCount.gt(viewCount).or(article.viewCount.eq(viewCount).and(article.id.gt(condition.getCursor())));
    }

    if ("commentCount".equals(orderBy)) {
      return article.id.lt(condition.getCursor());
    }

    LocalDateTime createdAt = cursorArticle.getCreatedAt();
    return direction == Order.DESC
        ? article.createdAt.lt(createdAt).or(article.createdAt.eq(createdAt).and(article.id.lt(condition.getCursor())))
        : article.createdAt.gt(createdAt).or(article.createdAt.eq(createdAt).and(article.id.gt(condition.getCursor())));
  }
  private OrderSpecifier<?>[] getOrderSpecifiers(ArticleSearchCondition condition) {
    List<OrderSpecifier<?>> specifiers = new ArrayList<>();

    // 1. 단수형 메서드를 호출해서 첫 번째 정렬 조건을 가져옴
    specifiers.add(getOrderSpecifier(condition));

    // 2. 보조 정렬 조건 (ID) 추가
    Order direction = condition.getDirection().equalsIgnoreCase("ASC") ? Order.ASC : Order.DESC;
    specifiers.add(new OrderSpecifier<>(direction, article.id));

    return specifiers.toArray(new OrderSpecifier[0]);
  }
}