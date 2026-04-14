package com.example.monew.domain.interest.repository;

import com.example.monew.domain.interest.entity.Interest;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import java.util.UUID;
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
    if (cursor == null || cursor.isBlank()) {
      return null;
    }
    int sep = cursor.lastIndexOf('|');
    if (sep <= 0 || sep == cursor.length() - 1) {
      return null;
    }
    String lastValue = cursor.substring(0, sep);
    UUID lastId;
    try {
      lastId = UUID.fromString(cursor.substring(sep + 1));
    } catch (IllegalArgumentException e) {
      return null;
    }
    boolean asc = !"desc".equalsIgnoreCase(direction);
    return (root, query, cb) -> {
      Expression<String> idAsString = root.<UUID>get("id").as(String.class);
      String lastIdStr = lastId.toString();
      Predicate tieBreaker = cb.greaterThan(idAsString, lastIdStr);
      if ("subscriberCount".equals(sortField)) {
        long lv = Long.parseLong(lastValue);
        Path<Long> expr = root.get("subscriberCount");
        Predicate strict = asc ? cb.greaterThan(expr, lv) : cb.lessThan(expr, lv);
        return cb.or(strict, cb.and(cb.equal(expr, lv), tieBreaker));
      }
      Path<String> expr = root.get("name");
      Predicate strict = asc ? cb.greaterThan(expr, lastValue) : cb.lessThan(expr, lastValue);
      return cb.or(strict, cb.and(cb.equal(expr, lastValue), tieBreaker));
    };
  }
}
