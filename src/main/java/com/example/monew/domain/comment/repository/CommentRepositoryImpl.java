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
import org.springframework.stereotype.Repository;

@Repository
public class CommentRepositoryImpl implements CommentRepositoryCustom{

  private final JPAQueryFactory queryFactory;
  private static final QCommentEntity comment = QCommentEntity.commentEntity;
  private static final QArticleEntity article = QArticleEntity.articleEntity;
  private static final QUser user = QUser.user;

  public CommentRepositoryImpl(EntityManager em) {
    this.queryFactory = new JPAQueryFactory(em);
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
      UUID cursorId,
      LocalDateTime cursorCreatedAt,
      Long cursorLikeCount,
      String sort,
      int size
  ) {

    BooleanExpression isLikedByMe = currentUserId != null ?
        JPAExpressions.selectOne()
            .from(commentLikeEntity)
            .where(commentLikeEntity.commentId.eq(comment.id)
                .and(commentLikeEntity.userId.eq(currentUserId)))
            .exists()
        : Expressions.asBoolean(false);

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
            getCursorCondition(cursorId, cursorCreatedAt, cursorLikeCount, sort)
        )
        .orderBy(getSortOrder(sort))
        .limit(size)
        .fetch();
  }


  @Override
  public long softDeleteAllByUserId(UUID userId) {
    return queryFactory.update(comment)
        .set(comment.deletedAt, LocalDateTime.now())
        .where(comment.userId.eq(userId), isNotDeleted())

        .execute();
  }

  @Override
  public long deleteAllByUserId(UUID userId) {
    return queryFactory.delete(comment)
        .where(comment.userId.eq(userId))
        .execute();
  }

  private BooleanExpression getCursorCondition(UUID cursorId, LocalDateTime cursorCreatedAt, Long cursorLikeCount, String sort) {
    if (cursorId == null) {
      return null;
    }
    // 좋아요 순 정렬일 때의 커서 조건
    if ("likeCount".equals(sort)) {
      if (cursorLikeCount == null || cursorCreatedAt == null) return null;

      return comment.likeCount.lt(cursorLikeCount)
          .or(comment.likeCount.eq(cursorLikeCount).and(comment.createdAt.lt(cursorCreatedAt)))
          .or(comment.likeCount.eq(cursorLikeCount).and(comment.createdAt.eq(cursorCreatedAt)).and(comment.id.lt(cursorId)));
    }

    // 기본 정렬(최신순)일 때의 커서 조건
    if (cursorCreatedAt == null) return null;

    return comment.createdAt.lt(cursorCreatedAt)
        .or(comment.createdAt.eq(cursorCreatedAt).and(comment.id.lt(cursorId)));
  }

  private OrderSpecifier<?>[] getSortOrder(String sort) {
    // 좋아요 순 정렬
    if ("likeCount".equals(sort)) {
      return new OrderSpecifier<?>[]{
          comment.likeCount.desc(),
          comment.createdAt.desc(),
          comment.id.desc()
      };
    }
    // 기본 정렬: 최신순
    return new OrderSpecifier<?>[]{
        comment.createdAt.desc(),
        comment.id.desc()
    };
  }
}

