package com.example.monew.domain.comment.repository;

import com.example.monew.domain.article.entity.QArticleEntity;
import com.example.monew.domain.activity.dto.CommentActivityDto;
import com.example.monew.domain.comment.entity.QCommentEntity;
import com.example.monew.domain.user.entity.QUser;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.querydsl.jpa.impl.JPAQuery;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class CommentRepositoryImpl implements CommentRepositoryCustom{

  private final JPAQueryFactory queryFactory;
  private final QCommentEntity comment = QCommentEntity.commentEntity;

  public CommentRepositoryImpl(EntityManager em) {
    this.queryFactory = new JPAQueryFactory(em);
  }

  @Override
  public List<CommentActivityDto> findCommentsByArticleWithCursor(
      UUID articleId, UUID cursorId, LocalDateTime cursorCreatedAt, Long cursorLikeCount, String sort, int size
  ) {
    QCommentEntity comment = QCommentEntity.commentEntity;
    QArticleEntity article = QArticleEntity.articleEntity;
    QUser user = QUser.user;

    JPAQuery<CommentActivityDto> query = queryFactory
        .select(Projections.constructor(CommentActivityDto.class,
            comment.id,
            article.id,
            //Expressions.constant(articleId),
            article.title,
            //Expressions.asString(""),
            user.id,
            user.nickname,
            comment.content,
            comment.likeCount,
            comment.createdAt
        ))
        .from(comment)
        .join(article).on(comment.articleId.eq(article.id))
        .join(user).on(comment.userId.eq(user.id))
        .where(
            comment.articleId.eq(articleId),
            cursorCondition(sort, cursorId, cursorCreatedAt, cursorLikeCount)
        )
        .limit(size + 1);

    if("LIKES".equalsIgnoreCase(sort)){
      query.orderBy(comment.likeCount.desc(), comment.createdAt.desc(), comment.id.desc());
    } else {
      query.orderBy(comment.createdAt.desc(), comment.id.desc());
    }
    return query.fetch();
  }

  private BooleanExpression cursorCondition(String sort, UUID cursorId, LocalDateTime cursorCreatedAt, Long cursorLikeCount) {
    if(cursorId == null) {
      return null;
    }

    QCommentEntity comment = QCommentEntity.commentEntity;

    if("LIKES".equalsIgnoreCase(sort) && cursorLikeCount != null) {
      return comment.likeCount.lt(cursorLikeCount)
          .or(comment.likeCount.eq(cursorLikeCount).and(comment.createdAt.lt(cursorCreatedAt)))
          .or(comment.likeCount.eq(cursorLikeCount).and(comment.createdAt.eq(cursorCreatedAt)).and(comment.id.lt(cursorId)));
    }

    return comment.createdAt.lt(cursorCreatedAt)
        .or(comment.createdAt.eq(cursorCreatedAt).and(comment.id.lt(cursorId)));
  }
}

