package com.example.monew.domain.user.repository;

import static com.example.monew.domain.user.entity.QUser.user;

import com.example.monew.domain.user.entity.User;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

import static com.example.monew.domain.user.entity.QUser.user;

@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepositoryCustom {
  private final JPAQueryFactory queryFactory;

  @Override
  public Optional<User> findActiveByEmail(String email) {
    return Optional.ofNullable(
        queryFactory.selectFrom(user)
            .where(user.email.eq(email), user.deletedAt.isNull())
            .fetchOne());
  }

  @Override
  public Optional<User> findActiveById(UUID id) {
    return Optional.ofNullable(
        queryFactory.selectFrom(user)
            .where(user.id.eq(id), user.deletedAt.isNull())
            .fetchOne());
  }

  @Override
  public List<User> findExpiredDeletedUsers(LocalDateTime threshold) {
    return queryFactory.selectFrom(user)
        .where(user.deletedAt.loe(threshold))
        .fetch();
  }
}