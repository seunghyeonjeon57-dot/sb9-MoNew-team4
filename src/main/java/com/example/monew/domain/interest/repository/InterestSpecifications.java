package com.example.monew.domain.interest.repository;

import com.example.monew.domain.interest.entity.Interest;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public final class InterestSpecifications {

  private InterestSpecifications() {
  }

  public static Specification<Interest> notDeleted() {
    return (root, query, cb) -> cb.isFalse(root.get("isDeleted"));
  }

  public static Specification<Interest> keywordContains(String keyword) {
    if (keyword == null || keyword.isBlank()) {
      return null;
    }
    String pattern = "%" + keyword.toLowerCase() + "%";
    return (root, query, cb) -> {
      if (query != null) {
        query.distinct(true);
      }
      Expression<String> nameLower = cb.lower(root.get("name"));
      Expression<String> keywordLower = cb.lower(
          root.joinList("keywords", JoinType.LEFT));
      return cb.or(cb.like(nameLower, pattern), cb.like(keywordLower, pattern));
    };
  }

  public static Specification<Interest> cursorAfter(
      String sortField, String direction, String cursor) {
    return InterestCursor.decode(cursor)
        .map(decoded -> buildCursorSpec(sortField, direction, decoded))
        .orElse(null);
  }

  private static Specification<Interest> buildCursorSpec(
      String sortField, String direction, InterestCursor cursor) {
    boolean asc = !"desc".equalsIgnoreCase(direction);
    return (root, query, cb) -> {
      Expression<String> idAsString = root.<java.util.UUID>get("id").as(String.class);
      Predicate tieBreaker = cb.greaterThan(idAsString, cursor.id().toString());
      if (InterestCursor.SUBSCRIBER_COUNT.equals(sortField)) {
        long lv = Long.parseLong(cursor.sortValue());
        Path<Long> expr = root.get("subscriberCount");
        Predicate strict = asc ? cb.greaterThan(expr, lv) : cb.lessThan(expr, lv);
        return cb.or(strict, cb.and(cb.equal(expr, lv), tieBreaker));
      }
      Path<String> expr = root.get("name");
      Predicate strict = asc
          ? cb.greaterThan(expr, cursor.sortValue())
          : cb.lessThan(expr, cursor.sortValue());
      return cb.or(strict, cb.and(cb.equal(expr, cursor.sortValue()), tieBreaker));
    };
  }
}
