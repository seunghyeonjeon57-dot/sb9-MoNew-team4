package com.example.monew.domain.activityManagement.service;


import static org.awaitility.Awaitility.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.example.monew.domain.activityManagement.dto.UserActivityDto;
import com.example.monew.domain.user.entity.User;
import com.example.monew.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.monew.domain.activityManagement.document.UserActivityDocument;
import com.example.monew.domain.activityManagement.repository.UserActivityRepository;


@ExtendWith(MockitoExtension.class)
public class ActivityServiceTest {

  @Mock
  private UserActivityRepository userActivityRepository;
  @Mock
  private UserRepository userRepository;

  @InjectMocks
  ActivityService activityService;

  @Test
  @DisplayName("처음 활동 내역을 조회하는 유저(데이터 없음)의 경우, 에러 없이 빈 리스트들을 가진 DTO를 반환한다.")
  void getUserActivity_ReturnEmptyDto_WhenNoDataExists() {
    UUID userId = UUID.randomUUID();
    String expectedEmail = "test@example.com";
    String expectedNickname = "뱀띠개발자";
    LocalDateTime expectedCreatedAt = LocalDateTime.now();

    User mockUser = mock(User.class);
    given(mockUser.getId()).willReturn(userId);
    given(mockUser.getEmail()).willReturn(expectedEmail);
    given(mockUser.getNickname()).willReturn(expectedNickname);
    given(mockUser.getCreatedAt()).willReturn(expectedCreatedAt);

    given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));

    given(userActivityRepository.findById(userId)).willReturn(Optional.empty());

    UserActivityDto result = activityService.getUserActivity(userId);

    assertThat(result).isNotNull();
    assertThat(result.id()).isEqualTo(userId);
    assertThat(result.email()).isEqualTo(expectedEmail);
    assertThat(result.nickname()).isEqualTo(expectedNickname);

    assertThat(result.subscribedInterests()).isEmpty();
    assertThat(result.comments()).isEmpty();
    assertThat(result.commentLikes()).isEmpty();
    assertThat(result.articleViews()).isEmpty();
  }

  @Test
  @DisplayName("기존 활동 내역이 있는 유저의 경우, 해당 도큐먼트의 데이터를 DTO로 정확히 매핑하여 반환한다.")
  void getUserActivity_ReturnMappedDto_WhenDataExists() {
    UUID userId = UUID.randomUUID();
    String expectedEmail = "test@test.com";
    String expectedNickname = "test";
    LocalDateTime expectedCreatedAt = LocalDateTime.now();

    User mockUser = mock(User.class);
    given(mockUser.getId()).willReturn(userId);
    given(mockUser.getEmail()).willReturn(expectedEmail);
    given(mockUser.getNickname()).willReturn(expectedNickname);
    given(mockUser.getCreatedAt()).willReturn(expectedCreatedAt);

    given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));

    UserActivityDocument mockDocument = UserActivityDocument.builder()
        .userId(userId)
        .build();

    given(userActivityRepository.findById(userId)).willReturn(Optional.of(mockDocument));

    UserActivityDto result = activityService.getUserActivity(userId);

    assertThat(result).isNotNull();
    assertThat(result.id()).isEqualTo(userId);
    assertThat(result.email()).isEqualTo(expectedEmail);
    assertThat(result.nickname()).isEqualTo(expectedNickname);
    assertThat(result.createdAt()).isEqualTo(expectedCreatedAt);

    verify(userRepository).findById(userId);
    verify(userActivityRepository).findById(userId);
  }

}
