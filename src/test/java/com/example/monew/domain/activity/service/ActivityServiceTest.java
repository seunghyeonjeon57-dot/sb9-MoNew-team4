package com.example.monew.domain.activity.service;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.monew.domain.activity.dto.CommentActivityDto;
import com.example.monew.domain.activity.dto.CommentLikeActivityDto;
import com.example.monew.domain.activity.dto.UserActivityDto;
import com.example.monew.domain.article.dto.ArticleViewDto;
import com.example.monew.domain.interest.dto.SubscriptionResponse;
import com.example.monew.domain.user.dto.UserDto;
import com.example.monew.domain.user.entity.User;
import com.example.monew.domain.user.entity.type.UserStatus;
import com.example.monew.domain.user.exception.UserNotFoundException;
import com.example.monew.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.example.monew.domain.activity.document.UserActivityDocument;
import com.example.monew.domain.activity.repository.UserActivityRepository;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.MongoTemplate;
import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;


@ExtendWith(MockitoExtension.class)
public class ActivityServiceTest {

  @Mock
  private UserActivityRepository userActivityRepository;
  @Mock
  private UserRepository userRepository;
  @Mock
  private RDBService rdbService;
  @Mock
  private MongoTemplate mongoTemplate;

  @InjectMocks
  ActivityService activityService;

  @Test
  @DisplayName("활동 내역 조회 성공 - DB와 몽고DB에 모두 데이터가 있을 때")
  void getUserActivity_Success() {
    UUID userId = UUID.randomUUID();

    User mockUser = User.builder()
        .email("test@monew.com")
        .nickname("모뉴테스터")
        .build();
    ReflectionTestUtils.setField(mockUser, "id", userId);
    ReflectionTestUtils.setField(mockUser, "createdAt", LocalDateTime.now());

    UserActivityDocument mockDocument = UserActivityDocument.builder()
        .userId(userId)
        .subscriptions(Collections.emptyList())
        .recentComments(Collections.emptyList())
        .build();

    given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
    given(userActivityRepository.findById(userId)).willReturn(Optional.of(mockDocument));

    UserActivityDto result = activityService.getUserActivity(userId);

    assertThat(result).isNotNull();
    assertThat(result.id()).isEqualTo(userId);
    assertThat(result.email()).isEqualTo("test@monew.com");
    assertThat(result.nickname()).isEqualTo("모뉴테스터");

    verify(userRepository).findById(userId);
    verify(userActivityRepository).findById(userId);
  }

  @Test
  @DisplayName("활동 내역 조회 성공 - 유저는 있지만 활동 내역(몽고DB)이 없을 때 빈 내역 반환")
  void getUserActivity_Success_EmptyDocument() {
    UUID userId = UUID.randomUUID();
    User mockUser = User.builder()
        .email("newbie@monew.com")
        .nickname("신규유저")
        .build();
    ReflectionTestUtils.setField(mockUser, "id", userId);

    given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
    given(userActivityRepository.findById(userId)).willReturn(Optional.empty());

    UserActivityDto result = activityService.getUserActivity(userId);

    assertThat(result).isNotNull();
    assertThat(result.id()).isEqualTo(userId);
    assertThat(result.subscriptions()).isEmpty();
    assertThat(result.comments()).isEmpty();
  }

  @Test
  @DisplayName("활동 내역 조회 실패 - 존재하지 않는 유저일 경우 예외 발생")
  void getUserActivity_Fail_UserNotFound() {
    UUID invalidUserId = UUID.randomUUID();
    given(userRepository.findById(invalidUserId)).willReturn(Optional.empty());

    assertThatThrownBy(() -> activityService.getUserActivity(invalidUserId))
        .isInstanceOf(UserNotFoundException.class)
        .hasMessageContaining("해당 유저를 찾을 수 없습니다.");

    verify(userActivityRepository, never()).findById(any());
  }

  @Test
  @DisplayName("MongoDB 활동 내역 업데이트 및 Upsert 호출 검증")
  void updateUser_Success() {
    UUID userId = UUID.randomUUID();
    UserDto userDto = new UserDto(
        userId,
        "test@test.com",
        "test",
        LocalDateTime.now().withNano(0)
    );

    ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
    ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);

    activityService.updateUser(userId, userDto);

    verify(mongoTemplate).upsert(
        queryCaptor.capture(),
        updateCaptor.capture(),
        org.mockito.ArgumentMatchers.eq(UserActivityDocument.class)
    );

    Query capturedQuery = queryCaptor.getValue();
    Update capturedUpdate = updateCaptor.getValue();

    assertThat(capturedQuery.getQueryObject().get("_id")).isEqualTo(userId);
    assertThat(capturedQuery.getQueryObject()).containsEntry("_id", userId);
  }

  @Test
  @DisplayName("MongoDB 업데이트 시 예외가 발생해도 로직이 중단되지 않고 로그를 남긴다")
  void updateUser_Fail_Logging() {
    UUID userId = UUID.randomUUID();
    UserDto userDto = new UserDto(userId, "test@test.com", "test", LocalDateTime.now());

    doThrow(new RuntimeException("MongoDB Connection Error"))
        .when(mongoTemplate).upsert(any(), any(), eq(UserActivityDocument.class));

    activityService.updateUser(userId, userDto);

    verify(mongoTemplate, times(1)).upsert(any(), any(), eq(UserActivityDocument.class));
  }


  @Test
  @DisplayName("사용자 활동 내역 논리 삭제 성공 테스트")
  void softDeleteUserActivity_Success() {
    UUID userId = UUID.randomUUID();
    long expectedDeletedCount = 1L;

    when(userActivityRepository.softDeleteAllByUserId(userId)).thenReturn(expectedDeletedCount);

    activityService.softDeleteUserActivity(userId);

    verify(userActivityRepository, times(1)).softDeleteAllByUserId(userId);
  }

  @Test
  @DisplayName("사용자 활동 내역 논리 삭제 중 예외 발생 시 로그 확인")
  void softDeleteUserActivity_Exception() {
    UUID userId = UUID.randomUUID();

    when(userActivityRepository.softDeleteAllByUserId(userId))
        .thenThrow(new RuntimeException("DB 삭제 오류"));

    assertDoesNotThrow(() -> activityService.softDeleteUserActivity(userId));

    verify(userActivityRepository, times(1)).softDeleteAllByUserId(userId);
  }

  @Test
  @DisplayName("성공: RDB 데이터를 기반으로 활동 내역을 동기화한다")
  void syncActivity_Success() {
    UUID userId = UUID.randomUUID();

    User user = User.builder()
        .nickname("hyeonhong")
        .email("kang@example.com")
        .password("encoded_password")
        .status(UserStatus.ACTIVE)
        .build();

    ReflectionTestUtils.setField(user, "id", userId);

    UserActivityDto expectedDto = UserActivityDto.builder()
        .id(userId)
        .email("kang@example.com")
        .nickname("test")
        .createdAt(LocalDateTime.now())
        .subscriptions(List.of())
        .comments(List.of())
        .commentLikes(List.of())
        .articleViews(List.of())
        .build();

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(rdbService.getUserActivity(userId)).willReturn(expectedDto);

    UserActivityDto result = activityService  .syncActivity(userId);

    assertThat(result).isNotNull();
    assertThat(result.id()).isEqualTo(userId);
    assertThat(result.nickname()).isEqualTo("test");

    verify(userRepository).findById(userId);
    verify(rdbService).getUserActivity(userId);
  }

  @Test
  @DisplayName("실패: 사용자가 존재하지 않으면 UserNotFoundException이 발생한다")
  void syncActivity_UserNotFound() {
    UUID userId = UUID.randomUUID();
    given(userRepository.findById(userId)).willReturn(Optional.empty());

    assertThrows(UserNotFoundException.class, () -> {
      activityService.syncActivity(userId);
    });

    verify(rdbService, never()).getUserActivity(any());
  }

  @Test
  @DisplayName("성공: RDB에서 가져온 최근 댓글 목록을 MongoDB에 동기화(upsert)한다")
  void syncRecentComments_Success() {
    UUID userId = UUID.randomUUID();
    List<CommentActivityDto> mockComments = List.of(
        CommentActivityDto.builder()
            .id(UUID.randomUUID())
            .content("테스트 댓글 1")
            .createdAt(LocalDateTime.now())
            .build(),
        CommentActivityDto.builder()
            .id(UUID.randomUUID())
            .content("테스트 댓글 2")
            .createdAt(LocalDateTime.now())
            .build()
    );

    given(rdbService.getRecentComments(userId)).willReturn(mockComments);

    ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
    ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);

    activityService.syncRecentComments(userId);

    verify(mongoTemplate).upsert(
        queryCaptor.capture(),
        updateCaptor.capture(),
        org.mockito.ArgumentMatchers.eq(UserActivityDocument.class)
    );

    Query capturedQuery = queryCaptor.getValue();
    Update capturedUpdate = updateCaptor.getValue();

    assertThat(capturedQuery.getQueryObject().get("_id")).isEqualTo(userId);
    assertThat(capturedUpdate.getUpdateObject().toString()).contains("recentComments");
  }

  @Test
  @DisplayName("성공: RDB에서 가져온 최근 좋아요 목록을 MongoDB에 동기화(upsert)한다")
  void syncRecentLikes_Success() {
    UUID userId = UUID.randomUUID();

    List<CommentLikeActivityDto> mockLikes = List.of(
        CommentLikeActivityDto.builder()
            .id(UUID.randomUUID())
            .createdAt(LocalDateTime.now())
            .commentId(UUID.randomUUID())
            .articleTitle("테스트 기사 제목 1")
            .commentContent("좋아요한 댓글 내용 1")
            .build(),
        CommentLikeActivityDto.builder()
            .id(UUID.randomUUID())
            .createdAt(LocalDateTime.now())
            .commentId(UUID.randomUUID())
            .articleTitle("테스트 기사 제목 2")
            .commentContent("좋아요한 댓글 내용 2")
            .build()
    );

    given(rdbService.getRecentLikes(userId)).willReturn(mockLikes);

    ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
    ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);

    activityService.syncRecentLikes(userId);

    verify(mongoTemplate, times(1)).upsert(
        queryCaptor.capture(),
        updateCaptor.capture(),
        org.mockito.ArgumentMatchers.eq(UserActivityDocument.class)
    );

    Query capturedQuery = queryCaptor.getValue();
    Update capturedUpdate = updateCaptor.getValue();

    assertThat(capturedQuery.getQueryObject().get("_id")).isEqualTo(userId);

    String updateJson = capturedUpdate.getUpdateObject().toString();
    assertThat(updateJson).contains("recentLikes");

    verify(rdbService).getRecentLikes(userId);
  }

  @Test
  @DisplayName("성공: RDB에서 가져온 최근 본 기사 목록을 MongoDB에 동기화(upsert)한다")
  void syncRecentArticles_Success() {
    UUID userId = UUID.randomUUID();

    List<ArticleViewDto> mockArticles = List.of(
        ArticleViewDto.builder()
            .id(UUID.randomUUID())
            .articleId(UUID.randomUUID())
            .articleTitle("최근 본 기사 제목 1")
            .createdAt(LocalDateTime.now())
            .build(),
        ArticleViewDto.builder()
            .id(UUID.randomUUID())
            .articleId(UUID.randomUUID())
            .articleTitle("최근 본 기사 제목 2")
            .createdAt(LocalDateTime.now())
            .build()
    );

    given(rdbService.getRecentArticles(userId)).willReturn(mockArticles);

    ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
    ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);

    activityService.syncRecentArticles(userId);

    verify(mongoTemplate, times(1)).upsert(
        queryCaptor.capture(),
        updateCaptor.capture(),
        org.mockito.ArgumentMatchers.eq(UserActivityDocument.class)
    );

    Query capturedQuery = queryCaptor.getValue();
    Update capturedUpdate = updateCaptor.getValue();

    assertThat(capturedQuery.getQueryObject()).containsEntry("_id", userId);

    String updateJson = capturedUpdate.getUpdateObject().toString();
    assertThat(updateJson).contains("recentArticles");

    verify(rdbService).getRecentArticles(userId);
  }

  @Test
  @DisplayName("성공: RDB에서 가져온 구독 목록을 MongoDB에 동기화(upsert)한다")
  void syncSubscriptions_Success() {
    UUID userId = UUID.randomUUID();

    List<SubscriptionResponse> mockSubscriptions = List.of(
        new SubscriptionResponse(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "IT/과학",
            List.of("AI", "반도체"),
            1500L,
            LocalDateTime.now()
        ),
        new SubscriptionResponse(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "경제",
            List.of("주식", "부동산"),
            2300L,
            LocalDateTime.now()
        )
    );

    given(rdbService.getSubscriptions(userId)).willReturn(mockSubscriptions);

    ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
    ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);

    activityService.syncSubscriptions(userId);

    verify(mongoTemplate, times(1)).upsert(
        queryCaptor.capture(),
        updateCaptor.capture(),
        org.mockito.ArgumentMatchers.eq(UserActivityDocument.class)
    );

    Query capturedQuery = queryCaptor.getValue();
    Update capturedUpdate = updateCaptor.getValue();

    assertThat(capturedQuery.getQueryObject()).containsEntry("_id", userId);

    String updateJson = capturedUpdate.getUpdateObject().toString();
    assertThat(updateJson).contains("subscriptions");

    verify(rdbService).getSubscriptions(userId);
  }

  @Test
  @DisplayName("성공: 여러 사용자 ID 리스트를 받아 각 사용자의 구독 정보를 동기화한다")
  void syncMultipleUsersSubscriptions_Success() {
    // Given
    UUID user1 = UUID.randomUUID();
    UUID user2 = UUID.randomUUID();
    List<UUID> userIds = List.of(user1, user2);

    given(rdbService.getSubscriptions(any(UUID.class)))
        .willReturn(Collections.emptyList());

    activityService.syncMultipleUsersSubscriptions(userIds);

    verify(rdbService, times(userIds.size())).getSubscriptions(any(UUID.class));

    verify(mongoTemplate).upsert(
        argThat(query -> query.getQueryObject().get("_id").equals(user1)),
        any(Update.class),
        eq(UserActivityDocument.class)
    );

    verify(mongoTemplate).upsert(
        argThat(query -> query.getQueryObject().get("_id").equals(user2)),
        any(Update.class),
        eq(UserActivityDocument.class)
    );
  }
}
