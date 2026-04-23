package com.example.monew.domain.comment.repository;

import static com.example.monew.domain.comment.entity.QCommentLikeEntity.commentLikeEntity;

import com.example.monew.domain.article.entity.QArticleEntity;
import com.example.monew.domain.comment.dto.CommentDto;
import com.example.monew.domain.comment.entity.QCommentEntity;
import com.example.monew.domain.user.entity.QUser;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import com.querydsl.core.types.Order;
import org.springframework.stereotype.Repository;

@Repository
public class CommentRepositoryImpl implements CommentRepositoryCustom{

  private final JPAQueryFactory queryFactory;
  private static final QCommentEntity comment = QCommentEntity.commentEntity;
  private static final QArticleEntity article = QArticleEntity.articleEntity;
  private static final QUser user = QUser.user;
  private static final String ORDER_BY_LIKE_COUNT = "likeCount";
  private final EntityManager em;


  public CommentRepositoryImpl(EntityManager em) {
    this.queryFactory = new JPAQueryFactory(em);
    this.em = em;
  }

  private BooleanExpression isNotDeleted() {
    return comment.deletedAt.isNull();
  }
  private BooleanExpression isArticleNotDeleted() {
    return article.deletedAt.isNull();
  }

  @Override
  public List<CommentDto> findCommentsByArticleWithCursor(
      UUID articleId,
      UUID currentUserId,
      String cursor,
      String orderBy,
      String direction,
      int size
  ) {
    UUID parsedCursorId = null;
    LocalDateTime parsedCreatedAt = null;
    Long parsedLikeCount = null;

    if (cursor != null && !cursor.isBlank()) {
      String[] parts = cursor.split("_");

      if (ORDER_BY_LIKE_COUNT.equals(orderBy) && parts.length >= 3) {
        parsedLikeCount = Long.parseLong(parts[0]);
        parsedCreatedAt = LocalDateTime.parse(parts[1]);
        parsedCursorId = UUID.fromString(parts[2]);
      } else if (parts.length >= 2) {
        parsedCreatedAt = LocalDateTime.parse(parts[0]);
        parsedCursorId = UUID.fromString(parts[1]);
      }
    }

    BooleanExpression isLikedByMe;

    if (currentUserId == null) {
      isLikedByMe = Expressions.asBoolean(false);
    } else {
      isLikedByMe = JPAExpressions.selectOne()
          .from(commentLikeEntity)
          .where(commentLikeEntity.commentId.eq(comment.id)
              .and(commentLikeEntity.userId.eq(currentUserId)))
          .exists();
    }

    return queryFactory
        .select(Projections.constructor(CommentDto.class,
            comment.id,
            comment.articleId,
            user.id,
            user.nickname,
            comment.content,
            comment.likeCount,
            isLikedByMe,
            comment.createdAt
        ))
        .from(comment)
        .join(user).on(comment.userId.eq(user.id))
        .join(article).on(comment.articleId.eq(article.id))
        .where(
            comment.articleId.eq(articleId),
            isArticleNotDeleted(),
            getCursorCondition(parsedCursorId, parsedCreatedAt, parsedLikeCount, orderBy, direction)
        )
        .orderBy(getSortOrder(orderBy, direction))
        .limit(size)
        .fetch();
  }


  @Override
  public long softDeleteAllByUserId(UUID userId) {
    long softDelete = queryFactory.update(comment)
        .set(comment.deletedAt, LocalDateTime.now())
        .where(comment.userId.eq(userId), isNotDeleted())
        .execute();

    em.flush();
    em.clear();
   return softDelete;
  }

  @Override
  public long deleteAllByUserId(UUID userId) {
    long delete = queryFactory.delete(comment)
        .where(comment.userId.eq(userId))
        .execute();
    em.flush();
    em.clear();
    return delete;
  }

  private BooleanExpression getCursorCondition(UUID cursorId, LocalDateTime cursorCreatedAt, Long cursorLikeCount, String orderBy, String direction) {
    if (cursorId == null) {
      return null;
    }

    boolean isAsc = "ASC".equalsIgnoreCase(direction);

    if (ORDER_BY_LIKE_COUNT.equals(orderBy)) {
      if (cursorLikeCount == null) return null;

      if (isAsc) {
        return comment.likeCount.gt(cursorLikeCount)
            .or(comment.likeCount.eq(cursorLikeCount).and(comment.id.gt(cursorId)));
      } else {
        return comment.likeCount.lt(cursorLikeCount)
            .or(comment.likeCount.eq(cursorLikeCount).and(comment.id.lt(cursorId)));
      }
    }

    if (cursorCreatedAt == null) return null;

    if (isAsc) {
      return comment.createdAt.gt(cursorCreatedAt)
          .or(comment.createdAt.eq(cursorCreatedAt).and(comment.id.gt(cursorId)));
    } else {
      return comment.createdAt.lt(cursorCreatedAt)
          .or(comment.createdAt.eq(cursorCreatedAt).and(comment.id.lt(cursorId)));
    }
  }

  private OrderSpecifier<?>[] getSortOrder(String orderBy, String direction) {
    Order order = "ASC".equalsIgnoreCase(direction) ? Order.ASC : Order.DESC;

    if (ORDER_BY_LIKE_COUNT.equals(orderBy)) {
      return new OrderSpecifier<?>[]{
          new OrderSpecifier<>(order, comment.likeCount),
          new OrderSpecifier<>(order, comment.id)
      };
    }

    return new OrderSpecifier<?>[]{
        new OrderSpecifier<>(order, comment.createdAt),
        new OrderSpecifier<>(order, comment.id)
    };
  }
}

