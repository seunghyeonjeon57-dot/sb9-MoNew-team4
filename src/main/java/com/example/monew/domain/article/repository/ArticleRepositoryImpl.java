package com.example.monew.domain.article.repository;

import static com.example.monew.domain.article.entity.QArticleEntity.articleEntity;

import com.example.monew.domain.article.dto.ArticleSearchCondition;
import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.article.entity.QArticleEntity;
import com.example.monew.domain.interest.entity.QInterest;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class ArticleRepositoryImpl implements ArticleRepositoryCustom {
  private final JPAQueryFactory queryFactory;
  private final EntityManager em;
  private final QArticleEntity article = articleEntity;

  public ArticleRepositoryImpl(EntityManager em) {
    this.queryFactory = new JPAQueryFactory(em);
    this.em = em;
  }

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

  @Override
  public List<ArticleEntity> findByCursor(ArticleSearchCondition condition) {
    int size = (condition.getSize() > 0) ? condition.getSize() : 10;

    return queryFactory
        .selectFrom(article)
        .leftJoin(article.interest, QInterest.interest).fetchJoin()
        .where(
            article.deletedAt.isNull(),
            allFilters(condition),
            cursorCondition(condition)
        )
        .orderBy(getOrderSpecifiers(condition))
        .limit(size + 1)
        .fetch();
  }

  private BooleanBuilder allFilters(ArticleSearchCondition condition) {
    BooleanBuilder builder = new BooleanBuilder();


    if (condition.getInterestId() != null) {

      QInterest interest = QInterest.interest;

      builder.and(
          article.interest.id.eq(condition.getInterestId())
              .or(
                  JPAExpressions.selectOne()
                      .from(interest)
                      .where(interest.id.eq(condition.getInterestId())
                          .and(article.title.containsIgnoreCase(interest.keywords.any().value)
                              .or(article.summary.containsIgnoreCase(interest.keywords.any().value))))
                      .exists()
              )
      );
    }

    // 2. 일반 검색어 필터 (기존 유지)
    if (condition.getKeyword() != null && !condition.getKeyword().isBlank()) {
      builder.and(keywordContains(condition.getKeyword()));
    }

    builder.and(sourceIn(condition.getSourceIn()));
    builder.and(publishDateBetween(condition.getPublishDateFrom(), condition.getPublishDateTo()));

    return builder;
  }
  private BooleanExpression sourceIn(List<String> sources) {
    if (sources == null || sources.isEmpty()) return null;
    return article.source.upper().in(
        sources.stream().map(String::toUpperCase).toList()
    );
  }

  private BooleanExpression publishDateBetween(LocalDateTime from, LocalDateTime to) {
    if (from == null && to == null) return null;
    if (from == null) return article.createdAt.loe(to);
    if (to == null) return article.createdAt.goe(from);
    return article.createdAt.between(from, to);
  }

  private OrderSpecifier<?> getOrderSpecifier(ArticleSearchCondition condition) {
    String dirStr = condition.getDirection();
    Order direction = "ASC".equalsIgnoreCase(dirStr) ? Order.ASC : Order.DESC;

    String orderBy = condition.getOrderBy();
    if (orderBy == null) orderBy = "createdAt";

    return switch (orderBy) {
      case "viewCount" -> new OrderSpecifier<>(direction, article.viewCount);
      case "commentCount" -> new OrderSpecifier<>(direction, article.id);
      default -> new OrderSpecifier<>(direction, article.createdAt);
    };
  }

  private BooleanExpression cursorCondition(ArticleSearchCondition condition) {
    if (condition.getCursor() == null || condition.getCursor().isBlank()) return null;

    UUID cursorId;
    try {
      cursorId = UUID.fromString(condition.getCursor());
    } catch (IllegalArgumentException e) {
      return null;
    }

    ArticleEntity cursorArticle = em.find(ArticleEntity.class, cursorId);
    if (cursorArticle == null) return null;

    String orderBy = (condition.getOrderBy() == null) ? "createdAt" : condition.getOrderBy();
    boolean isDesc = !"ASC".equalsIgnoreCase(condition.getDirection());

    if ("viewCount".equals(orderBy)) {
      return viewCountCursorCondition(cursorArticle, cursorId, isDesc);
    }

    if ("commentCount".equals(orderBy)) {
      return article.id.lt(cursorId);
    }

    return createdAtCursorCondition(cursorArticle, cursorId, isDesc);
  }

  private BooleanExpression viewCountCursorCondition(
      ArticleEntity cursorArticle, UUID cursorId, boolean isDesc) {

    long v = cursorArticle.getViewCount();

    if (isDesc) {
      return article.viewCount.lt(v)
          .or(article.viewCount.eq(v).and(article.id.lt(cursorId)));
    } else {
      return article.viewCount.gt(v)
          .or(article.viewCount.eq(v).and(article.id.gt(cursorId)));
    }
  }

  private BooleanExpression createdAtCursorCondition(
      ArticleEntity cursorArticle, UUID cursorId, boolean isDesc) {

    LocalDateTime t = cursorArticle.getCreatedAt();

    if (isDesc) {
      return article.createdAt.lt(t)
          .or(article.createdAt.eq(t).and(article.id.lt(cursorId)));
    } else {
      return article.createdAt.gt(t)
          .or(article.createdAt.eq(t).and(article.id.gt(cursorId)));
    }
  }

  private BooleanExpression keywordContains(String keyword) {
    if (keyword == null || keyword.isBlank()) return null;

    return article.title.containsIgnoreCase(keyword)
        .or(article.summary.containsIgnoreCase(keyword))
        .or(article.interest.keywords.any().value.containsIgnoreCase(keyword));
  }

  private OrderSpecifier<?>[] getOrderSpecifiers(ArticleSearchCondition condition) {
    List<OrderSpecifier<?>> specifiers = new ArrayList<>();
    specifiers.add(getOrderSpecifier(condition));

    Order direction = "ASC".equalsIgnoreCase(condition.getDirection()) ? Order.ASC : Order.DESC;
    specifiers.add(new OrderSpecifier<>(direction, article.id));

    return specifiers.toArray(new OrderSpecifier[0]);
  }
}