package com.example.monew.domain.activity.service;

import com.example.monew.domain.activity.dto.CommentActivityDto;
import com.example.monew.domain.activity.dto.CommentLikeActivityDto;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.monew.domain.activity.document.UserActivityDocument;
import com.example.monew.domain.activity.dto.UserActivityDto;
import com.example.monew.domain.activity.repository.UserActivityRepository;
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
        .subscriptions(document.getSubscriptions())
        .comments(document.getRecentComments())
        .commentLikes(document.getRecentLikes())
        .articleViews(document.getRecentArticles())
        .build();
  }

  public void updateRecentComments(UUID userId, CommentActivityDto commentDto) {
    try {
      Query query = new Query(Criteria.where("_id").is(userId));

      Update update = new Update();
      update.push("recentComments")
          .atPosition(Position.FIRST)    // 배열의 최신
          .slice(10)              // 최신 10개 유지
          .each(commentDto);            // 추가할 데이터

      mongoTemplate.upsert(query, update, UserActivityDocument.class);
      log.info("MongoDB 활동 내역 업데이트 성공: userId={}", userId);
    } catch (Exception e) {
      log.warn("MongoDB 활동 내역 업데이트 실패 (데이터 정합성 보정 필요): userId={}, error={}", userId, e.getMessage());
    }
  }

  public void updateRecentLikedComments(UUID userId, CommentLikeActivityDto commentDto) {
    try {
      Query query = new Query(Criteria.where("_id").is(userId));

      Update update = new Update().push("recentLikes")
          .atPosition(Update.Position.FIRST)
          .slice(10)
          .each(commentDto);

      mongoTemplate.upsert(query, update, UserActivityDocument.class);
      log.info("MongoDB 활동 내역 업데이트 성공: userId={}", userId);
    } catch (Exception e) {
      log.warn("MongoDB 활동 내역 업데이트 실패 (데이터 정합성 보정 필요): userId={}, error={}", userId, e.getMessage());
    }
  }

  public void updateRecentViewedArticles(UUID userId, ArticleViewDto articleDto) {
    try {
      Query query = new Query(Criteria.where("_id").is(userId));

      Update update = new Update().push("recentArticles")
          .atPosition(Update.Position.FIRST)
          .slice(10)
          .each(articleDto);

      mongoTemplate.upsert(query, update, UserActivityDocument.class);
      log.info("MongoDB 활동 내역 업데이트 성공: userId={}", userId);
    } catch (Exception e) {
      log.warn("MongoDB 활동 내역 업데이트 실패 (데이터 정합성 보정 필요): userId={}, error={}", userId, e.getMessage());
    }
  }

  public void updateUser(UUID userId, UserDto userDto){
    try{
      Query query = new Query(Criteria.where("_id").is(userId));

      Update update = new Update().set("userProfile", userDto);

      mongoTemplate.upsert(query, update, UserActivityDocument.class);
      log.info("MongoDB 활동 내역 업데이트 성공: userId={}", userId);
    } catch (Exception e) {
      log.warn("MongoDB 활동 내역 업데이트 실패 (데이터 정합성 보정 필요): userId={}, error={}", userId, e.getMessage());
    }
  }

  public void updateSubscriptionResponse(UUID userId, SubscriptionResponse subscriptionResponse) {
    try{
      Query query = new Query(Criteria.where("_id").is(userId));

      Update update = new Update().addToSet("subscriptions", subscriptionResponse);

      mongoTemplate.upsert(query, update, UserActivityDocument.class);
      log.info("MongoDB 활동 내역 업데이트 성공: userId={}", userId);
    } catch (Exception e) {
      log.warn("MongoDB 활동 내역 업데이트 실패 (데이터 정합성 보정 필요): userId={}, error={}", userId, e.getMessage());
    }
  }

  public void deleteUserActivity(UUID userId) {
    try {
      userActivityRepository.deleteAllByUserId(userId);
      log.info("MongoDB 사용자 활동 내역 삭제 성공: userId={}", userId);
    } catch (Exception e) {
      log.error("MongoDB 사용자 활동 내역 삭제 실패: userId={}, error={}", userId, e.getMessage());
    }
  }

  public void softDeleteUserActivity(UUID userId) {
    try {
      long softDeletedCount = userActivityRepository.softDeleteAllByUserId(userId);
      log.info("MongoDB 사용자 활동 내역 논리 삭제 성공: userId={}, 처리된 문서 수={}", userId, softDeletedCount);
    } catch (Exception e) {
      log.error("MongoDB 사용자 활동 내역 논리 삭제 실패: userId={}, error={}", userId, e.getMessage());
    }
  }

  public void removeSubscription(UUID userId, UUID interestId) {
    try {
      Query query = new Query(Criteria.where("_id").is(userId));

      Update update = new Update().pull("subscriptions",
          new org.bson.Document("interestId", interestId)
      );

      mongoTemplate.updateFirst(query, update, UserActivityDocument.class);
      log.info("MongoDB 관심사 구독 취소 반영 성공: userId={}, interestId={}", userId, interestId);

    } catch (Exception e) {
      log.warn("MongoDB 관심사 구독 취소 반영 실패: userId={}, interestId={}, error={}", userId, interestId, e.getMessage());
    }
  }

  public void commentLikeCountInRecentComments(UUID userId, UUID commentId, Long newLikeCount) {
    try {
      Query query = new Query(Criteria.where("_id").is(userId)
          .and("recentComments.id").is(commentId));

      Update update = new Update().set("recentComments.$.likeCount", newLikeCount);

      var result = mongoTemplate.updateFirst(query, update, UserActivityDocument.class);

      if (result.getModifiedCount() > 0) {
        log.info("MongoDB 내가 쓴 댓글 좋아요 수 동기화 성공: commentId={}", commentId);
      }
    } catch (Exception e) {
      log.warn("MongoDB 내가 쓴 댓글 좋아요 수 동기화 실패: commentId={}, error={}", commentId, e.getMessage());
    }
  }
}
