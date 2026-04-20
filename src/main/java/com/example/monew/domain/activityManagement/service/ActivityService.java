package com.example.monew.domain.activityManagement.service;

import com.example.monew.domain.user.exception.UserNotFoundException;
import java.util.UUID;

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
}
