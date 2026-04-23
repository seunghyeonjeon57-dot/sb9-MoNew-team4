package com.example.monew.domain.activityManagement.service;

import com.example.monew.domain.activityManagement.dto.CommentActivityDto;
import com.example.monew.domain.activityManagement.dto.CommentLikeActivityDto;
import com.example.monew.domain.article.dto.ArticleViewDto;
import com.example.monew.domain.interest.dto.SubscriptionResponse;
import com.example.monew.domain.user.dto.UserDto;
import com.example.monew.domain.user.exception.UserNotFoundException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import java.util.UUID;

import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update.Position;
import org.springframework.data.mongodb.core.query.UpdateDefinition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.monew.domain.activityManagement.document.UserActivityDocument;
import com.example.monew.domain.activityManagement.dto.UserActivityDto;
import com.example.monew.domain.activityManagement.repository.UserActivityRepository;
import com.example.monew.domain.user.entity.User;
import com.example.monew.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityService {

  private final UserActivityRepository userActivityRepository;
  private final UserRepository userRepository;
  private final MongoTemplate mongoTemplate;

  @Transactional(readOnly = true)
  public UserActivityDto getUserActivity(UUID userId) {

  log.info("사용자 활동 내역 조회 시도: userId={}", userId);

  User user = userRepository.findById(userId)
      .orElseThrow(() -> {
        log.warn("활동 내역 조회 실패: 존재하지 않는 사용자 userId={}", userId);
        return new UserNotFoundException("해당 유저를 찾을 수 없습니다.");
      });

  UserActivityDocument document = userActivityRepository.findById(userId)
      .orElseGet(() -> {
        log.info("활동 내역이 없어 빈 내역을 반환합니다. userId={}", userId);
        return UserActivityDocument.builder()
            .userId(userId)
            .build();
      });

  log.info("사용자 활동 내역 조회 완료: userId={}", userId);

    return UserActivityDto.builder()
        .id(user.getId())
        .email(user.getEmail())
        .nickname(user.getNickname())
        .createdAt(user.getCreatedAt())
        .subscribedInterests(document.getSubscribedInterests())
        .comments(document.getRecentComments())
        .commentLikes(document.getRecentLikes())
        .articleViews(document.getRecentArticles())
        .build();
  }

  public void updateRecentComments(UUID userId, CommentActivityDto commentDto) {
    Query query = new Query(Criteria.where("_id").is(userId));

    Update update = new Update();
    update.push("recentComments")
        .atPosition(Position.FIRST)    // 배열의 최신
        .slice(10)              // 최신 10개 유지
        .each(commentDto);            // 추가할 데이터

    mongoTemplate.upsert(query, (UpdateDefinition) update, UserActivityDocument.class);
  }

  public void updateRecentLikedComments(UUID userId, CommentLikeActivityDto commentDto) {
    Query query = new Query(Criteria.where("_id").is(userId));

    Update update = new Update().push("recentLikedComments")
        .atPosition(Update.Position.FIRST)
        .slice(10)
        .each(commentDto);

    mongoTemplate.upsert(query, (UpdateDefinition) update, UserActivityDocument.class);
  }

  public void updateRecentViewedArticles(UUID userId, ArticleViewDto articleDto) {
    Query query = new Query(Criteria.where("_id").is(userId));

    Update update = new Update().push("recentViewedArticles")
        .atPosition(Update.Position.FIRST)
        .slice(10)
        .each(articleDto);

    mongoTemplate.upsert(query, update, UserActivityDocument.class);
  }

  public void updateUser(UUID userId, UserDto userDto){
    Query query = new Query(Criteria.where("_id").is(userId));

    Update update = new Update().set("userProfile", userDto);

    mongoTemplate.upsert(query, update, UserActivityDocument.class);
  }

  public void updateSubscriptionResponse(UUID userId, SubscriptionResponse subscriptionResponse) {
    Query query = new Query(Criteria.where("_id").is(userId));

    Update update = new Update().addToSet("subscribedInterests", subscriptionResponse);

    mongoTemplate.upsert(query, update, UserActivityDocument.class);
  }
}
