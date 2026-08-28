package com.example.monew.domain.activity.service;

import com.example.monew.domain.user.dto.UserDto;
import com.example.monew.domain.user.exception.UserNotFoundException;
import java.util.Collection;
import java.util.List;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import java.util.UUID;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.query.Criteria;
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
  private final RDBService rdbService;
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


  public void deleteUserActivity(UUID userId) {
    try {
      userActivityRepository.deleteAllByUserId(userId);
      log.info("MongoDB 사용자 활동 내역 삭제 성공: userId={}", userId);
    } catch (Exception e) {
      log.error("MongoDB 사용자 활동 내역 삭제 실패: userId={}, error={}", userId, e.getMessage());
    }
  }

  public void deleteUserActivities(Collection<UUID> userIds) {
    try {
      long deletedCount = userActivityRepository.deleteAllByUserIdIn(userIds);
      log.info("MongoDB 사용자 활동 내역 일괄 삭제 성공: 대상 {}명, 삭제된 문서 {}건", userIds.size(), deletedCount);
    } catch (Exception e) {
      log.error("MongoDB 사용자 활동 내역 일괄 삭제 실패: 대상 {}명, error={}", userIds.size(), e.getMessage());
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


  @Transactional(readOnly = true)
  public UserActivityDto syncActivity(UUID userId) {

    log.info("RDB 실시간 조회를 통한 활동 내역 생성: userId={}", userId);

    userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

    return rdbService.getUserActivity(userId);
  }

  public void syncRecentComments(UUID userId) {
    var comments = rdbService.getRecentComments(userId);

    Query query = new Query(Criteria.where("_id").is(userId));
    Update update = new Update().set("recentComments", comments);

    mongoTemplate.upsert(query, update, UserActivityDocument.class);
    log.info("MongoDB 댓글 동기화 완료: userId={}", userId);
  }

  public void syncRecentLikes(UUID userId) {
    var likes = rdbService.getRecentLikes(userId);

    Query query = new Query(Criteria.where("_id").is(userId));
    Update update = new Update().set("recentLikes", likes);

    mongoTemplate.upsert(query, update, UserActivityDocument.class);
    log.info("MongoDB 좋아요 동기화 완료: userId={}", userId);
  }

  public void syncRecentArticles(UUID userId) {
    var articles = rdbService.getRecentArticles(userId);

    Query query = new Query(Criteria.where("_id").is(userId));
    Update update = new Update().set("recentArticles", articles);

    mongoTemplate.upsert(query, update, UserActivityDocument.class);
    log.info("MongoDB 최근 본 기사 동기화 완료: userId={}", userId);
  }

  public void syncSubscriptions(UUID userId) {
    var subscriptions = rdbService.getSubscriptions(userId);

    Query query = new Query(Criteria.where("_id").is(userId));
    Update update = new Update().set("subscriptions", subscriptions);

    mongoTemplate.upsert(query, update, UserActivityDocument.class);
  }

  public void syncMultipleUsersSubscriptions(List<UUID> userIds) {
    for (UUID userId : userIds) {
      syncSubscriptions(userId);
    }
  }
}
