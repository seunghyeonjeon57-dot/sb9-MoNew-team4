package com.example.monew.domain.interest.repository;

import com.example.monew.domain.interest.entity.Interest;
import com.example.monew.domain.interest.entity.QInterest;
import com.example.monew.domain.interest.entity.QInterestKeyword;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class InterestRepositoryImpl implements InterestRepositoryCustom {

  private final JPAQueryFactory queryFactory;
  private final QInterest interest = QInterest.interest;
  private final QInterestKeyword keyword = QInterestKeyword.interestKeyword;

  public InterestRepositoryImpl(EntityManager em) {
    this.queryFactory = new JPAQueryFactory(em);
  }

  @Override
  public CursorPage findByCursor(
      String keywordArg, String orderBy, String direction,
      UUID cursorId, LocalDateTime after, int limit) {

    BooleanBuilder base = new BooleanBuilder();
    base.and(interest.deletedAt.isNull());
    BooleanExpression keywordMatch = keywordCondition(keywordArg);
    if (keywordMatch != null) {
      base.and(keywordMatch);
    }

    BooleanBuilder pageWhere = new BooleanBuilder().and(base);
    BooleanExpression cursorWhere = buildCursorWhere(orderBy, direction, cursorId, after);
    if (cursorWhere != null) {
      pageWhere.and(cursorWhere);
    }

    List<Interest> rows = queryFactory.selectFrom(interest)
        .where(pageWhere)
        .orderBy(buildOrder(orderBy, direction))
        .limit(limit + 1L)
        .fetch();

    Long totalBoxed = queryFactory.select(interest.count())
        .from(interest)
        .where(base)
        .fetchOne();
    long total = totalBoxed == null ? 0L : totalBoxed;

    boolean hasNext = rows.size() > limit;
    List<Interest> content = hasNext ? rows.subList(0, limit) : rows;
    return new CursorPage(content, total, hasNext);
  }

  private BooleanExpression keywordCondition(String kw) {
    if (kw == null || kw.isBlank()) {
      return null;
    }
    return interest.name.containsIgnoreCase(kw)
        .or(JPAExpressions.selectOne()
            .from(keyword)
            .where(keyword.interest.id.eq(interest.id)
                .and(keyword.value.containsIgnoreCase(kw)))
            .exists());
  }

  private BooleanExpression buildCursorWhere(
      String orderBy, String direction, UUID cursorId, LocalDateTime after) {
    if (cursorId == null && after == null) {
      return null;
    }
    if (cursorId != null) {
      Interest anchor = queryFactory.selectFrom(interest)
          .where(interest.id.eq(cursorId).and(interest.deletedAt.isNull()))
          .fetchOne();
      if (anchor != null) {
        return fullKeyset(orderBy, direction, anchor);
      }
      if (after != null) {
        return timestampKeyset(after, cursorId);
      }
      return null;
    }
    return interest.createdAt.gt(after);
  }

  private BooleanExpression fullKeyset(String orderBy, String direction, Interest anchor) {
    LocalDateTime ac = anchor.getCreatedAt();
    UUID aid = anchor.getId();
    boolean desc = "DESC".equals(direction);

    if ("subscriberCount".equals(orderBy)) {
      long av = anchor.getSubscriberCount();
      BooleanExpression primary = desc
          ? interest.subscriberCount.lt(av)
          : interest.subscriberCount.gt(av);
      return primary
          .or(interest.subscriberCount.eq(av).and(interest.createdAt.gt(ac)))
          .or(interest.subscriberCount.eq(av).and(interest.createdAt.eq(ac)).and(interest.id.gt(aid)));
    }

    String an = anchor.getName();
    BooleanExpression primary = desc ? interest.name.lt(an) : interest.name.gt(an);
    return primary
        .or(interest.name.eq(an).and(interest.createdAt.gt(ac)))
        .or(interest.name.eq(an).and(interest.createdAt.eq(ac)).and(interest.id.gt(aid)));
  }

  private BooleanExpression timestampKeyset(LocalDateTime after, UUID cursorId) {
    return interest.createdAt.gt(after)
        .or(interest.createdAt.eq(after).and(interest.id.gt(cursorId)));
  }

  private OrderSpecifier<?>[] buildOrder(String orderBy, String direction) {
    boolean desc = "DESC".equals(direction);
    OrderSpecifier<?> primary;
    if ("subscriberCount".equals(orderBy)) {
      primary = desc ? interest.subscriberCount.desc() : interest.subscriberCount.asc();
    } else {
      primary = desc ? interest.name.desc() : interest.name.asc();
    }
    return new OrderSpecifier<?>[]{primary, interest.createdAt.asc(), interest.id.asc()};
  }
}
