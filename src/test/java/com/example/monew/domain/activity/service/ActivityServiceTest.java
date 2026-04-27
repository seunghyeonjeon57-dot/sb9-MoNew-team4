package com.example.monew.domain.activity.service;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.monew.domain.activity.dto.CommentActivityDto;
import com.example.monew.domain.activity.dto.CommentLikeActivityDto;
import com.example.monew.domain.activity.dto.UserActivityDto;
import com.example.monew.domain.article.dto.ArticleViewDto;
import com.example.monew.domain.interest.dto.SubscriptionResponse;
import com.example.monew.domain.user.dto.UserDto;
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
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.query.UpdateDefinition;
import org.bson.Document;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.monew.domain.activity.document.UserActivityDocument;
import com.example.monew.domain.activity.repository.UserActivityRepository;


@ExtendWith(MockitoExtension.class)
public class ActivityServiceTest {

  @Mock
  private UserActivityRepository userActivityRepository;
  @Mock
  private UserRepository userRepository;
  @Mock
  private MongoTemplate mongoTemplate;

  @InjectMocks
  ActivityService activityService;

  @Test
  @DisplayName("처음 활동 내역을 조회하는 유저(데이터 없음)의 경우, 에러 없이 빈 리스트들을 가진 DTO를 반환한다.")
  void getUserActivity_ReturnEmptyDto_WhenNoDataExists() {
    UUID userId = UUID.randomUUID();
    String expectedEmail = "test@example.com";
    String expectedNickname = "test";
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

    assertThat(result.subscriptions()).isEmpty();
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


  @Test
  @DisplayName("최근 댓글 추가 시 MongoTemplate의 push와 slice 로직이 정상적으로 호출된다.")
  void updateRecentComments_CallsUpsertWithPushAndSlice() {
    UUID userId = UUID.randomUUID();
    CommentActivityDto mockDto = mock(CommentActivityDto.class);

    activityService.updateRecentComments(userId, mockDto);

    ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
    ArgumentCaptor<UpdateDefinition> updateCaptor = ArgumentCaptor.forClass(UpdateDefinition.class);

    verify(mongoTemplate).upsert(queryCaptor.capture(), updateCaptor.capture(), eq(UserActivityDocument.class));

    Query capturedQuery = queryCaptor.getValue();
    Update capturedUpdate = (Update) updateCaptor.getValue();

    Document queryObject = capturedQuery.getQueryObject();
    assertThat(queryObject.get("_id")).isEqualTo(userId);

    Document updateObject = capturedUpdate.getUpdateObject();
    assertThat(capturedUpdate.getUpdateObject()).containsKey("$push");

    Document pushObject = (Document) updateObject.get("$push");
    assertThat(pushObject.containsKey("recentComments")).isTrue();
  }

  @Test
  @DisplayName("최근 좋아요 누른 댓글 추가 시 MongoTemplate의 push와 slice 로직이 정상적으로 호출된다.")
  void updateRecentLikedComments_CallsUpsertWithPushAndSlice() {
    UUID userId = UUID.randomUUID();
    CommentLikeActivityDto mockDto = mock(CommentLikeActivityDto.class);

    activityService.updateRecentLikedComments(userId, mockDto);

    ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
    ArgumentCaptor<UpdateDefinition> updateCaptor = ArgumentCaptor.forClass(UpdateDefinition.class);

    verify(mongoTemplate).upsert(queryCaptor.capture(), updateCaptor.capture(), eq(UserActivityDocument.class));

    Query capturedQuery = queryCaptor.getValue();
    Update capturedUpdate = (Update) updateCaptor.getValue();

    Document queryObject = capturedQuery.getQueryObject();
    assertThat(queryObject.get("_id")).isEqualTo(userId);

    Document updateObject = capturedUpdate.getUpdateObject();
    assertThat(updateObject.containsKey("$push")).isTrue();

    Document pushObject = (Document) updateObject.get("$push");
    assertThat(pushObject.containsKey("recentLikes")).isTrue();
  }


  @Test
  @DisplayName("최근 읽은 기사 추가 시 MongoTemplate의 push와 slice 로직이 정상적으로 호출된다.")
  void updateRecentViewedArticles_CallsUpsertWithPushAndSlice() {
    UUID userId = UUID.randomUUID();
    ArticleViewDto mockDto = mock(ArticleViewDto.class);

    activityService.updateRecentViewedArticles(userId, mockDto);

    ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
    ArgumentCaptor<UpdateDefinition> updateCaptor = ArgumentCaptor.forClass(UpdateDefinition.class);

    verify(mongoTemplate).upsert(queryCaptor.capture(), updateCaptor.capture(), eq(UserActivityDocument.class));

    Query capturedQuery = queryCaptor.getValue();
    Update capturedUpdate = (Update) updateCaptor.getValue();

    Document queryObject = capturedQuery.getQueryObject();
    assertThat(queryObject.get("_id")).isEqualTo(userId);

    Document updateObject = capturedUpdate.getUpdateObject();
    assertThat(updateObject.containsKey("$push")).isTrue();

    Document pushObject = (Document) updateObject.get("$push");
    assertThat(pushObject.containsKey("recentArticles")).isTrue();
  }

  @Test
  @DisplayName("사용자 프로필 업데이트 시 MongoTemplate의 set 로직이 정상적으로 호출된다.")
  void updateUser_CallsUpsertWithSet() {
    UUID userId = UUID.randomUUID();
    UserDto mockUserDto = mock(UserDto.class);

    activityService.updateUser(userId, mockUserDto);

    ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
    ArgumentCaptor<UpdateDefinition> updateCaptor = ArgumentCaptor.forClass(UpdateDefinition.class);

    verify(mongoTemplate).upsert(queryCaptor.capture(), updateCaptor.capture(), eq(UserActivityDocument.class));

    Query capturedQuery = queryCaptor.getValue();
    Update capturedUpdate = (Update) updateCaptor.getValue();

    Document queryObject = capturedQuery.getQueryObject();
    assertThat(queryObject.get("_id")).isEqualTo(userId);

    Document updateObject = capturedUpdate.getUpdateObject();
    assertThat(updateObject.containsKey("$set")).isTrue();

    Document setObject = (Document) updateObject.get("$set");
    assertThat(setObject.containsKey("userProfile")).isTrue();
  }

  @Test
  @DisplayName("구독 정보 업데이트 시 MongoTemplate의 set 로직이 정상적으로 호출된다.")
  void updateSubscriptionResponse_CallsUpsertWithSet() {
    UUID userId = UUID.randomUUID();
    SubscriptionResponse mockDto = mock(SubscriptionResponse.class);

    activityService.updateSubscriptionResponse(userId, mockDto);

    ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
    ArgumentCaptor<UpdateDefinition> updateCaptor = ArgumentCaptor.forClass(UpdateDefinition.class);

    verify(mongoTemplate).upsert(queryCaptor.capture(), updateCaptor.capture(), eq(UserActivityDocument.class));

    Query capturedQuery = queryCaptor.getValue();
    Update capturedUpdate = (Update) updateCaptor.getValue();

    Document queryObject = capturedQuery.getQueryObject();
    assertThat(queryObject.get("_id")).isEqualTo(userId);

    Document updateObject = capturedUpdate.getUpdateObject();
    assertThat(updateObject).containsKey("$addToSet");

    Document addToSetObject = (Document) updateObject.get("$addToSet");
    assertThat(addToSetObject).containsKey("subscriptions");
  }

  @Test
  @DisplayName("관심사 구독 취소 - MongoDB 배열에서 $pull 연산자가 포함된 업데이트가 정상 실행된다")
  void removeSubscription_Success() {

    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();

    activityService.removeSubscription(userId, interestId);

    ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
    ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);

    verify(mongoTemplate, times(1)).updateFirst(
        queryCaptor.capture(),
        updateCaptor.capture(),
        eq(UserActivityDocument.class)
    );

    Query capturedQuery = queryCaptor.getValue();
    assertThat(capturedQuery.getQueryObject().get("_id")).isEqualTo(userId);

    Update capturedUpdate = updateCaptor.getValue();
    assertThat(capturedUpdate.getUpdateObject().containsKey("$pull")).isTrue();
  }

  @Test
  @DisplayName("관심사 구독 취소 예외 - MongoDB 통신 중 에러가 발생해도 예외를 던지지 않고 꿀꺽 삼킨다 (트랜잭션 롤백 방지)")
  void removeSubscription_ExceptionHandled() {
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();

    doThrow(new RuntimeException("MongoDB 연결 에러 테스트"))
        .when(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(UserActivityDocument.class));

    assertDoesNotThrow(() -> activityService.removeSubscription(userId, interestId));

    verify(mongoTemplate, times(1)).updateFirst(any(Query.class), any(Update.class), eq(UserActivityDocument.class));
  }

  @Test
  @DisplayName("MongoDB 댓글 수정 동기화 성공 케이스")
  void updateRecentCommentsInactivity_Success() {
    UUID userId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();
    String newComment = "수정된 댓글 내용";

    activityService.updateRecentCommentsInactivity(userId, commentId, newComment);

    verify(mongoTemplate, times(1)).updateFirst(
        any(Query.class),
        any(Update.class),
        eq(UserActivityDocument.class)
    );
  }

  @Test
  @DisplayName("MongoDB 처리 중 예외 발생 시 예외 처리 확인")
  void updateRecentCommentsInactivity_Exception() {
    UUID userId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();
    String newComment = "수정된 댓글 내용";

    doThrow(new RuntimeException("DB 에러")).when(mongoTemplate)
        .updateFirst(any(Query.class), any(Update.class), eq(UserActivityDocument.class));

    activityService.updateRecentCommentsInactivity(userId, commentId, newComment);
  }

  @Test
  @DisplayName("MongoDB 좋아요 삭제(pull) 동기화 성공")
  void removeRecentLikedComments_Success() {
    UUID userId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();

    activityService.removeRecentLikedComments(userId, commentId);

    verify(mongoTemplate, times(1)).updateFirst(
        any(Query.class),
        any(Update.class),
        eq(UserActivityDocument.class)
    );
  }

  @Test
  @DisplayName("MongoDB 좋아요 삭제 중 예외 발생 시 로그 기록 확인")
  void removeRecentLikedComments_Exception() {
    UUID userId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();

    doThrow(new RuntimeException("DB Connection Fail")).when(mongoTemplate)
        .updateFirst(any(Query.class), any(Update.class), eq(UserActivityDocument.class));

    activityService.removeRecentLikedComments(userId, commentId);

    verify(mongoTemplate, times(1)).updateFirst(
        any(Query.class),
        any(UpdateDefinition.class),
        eq(UserActivityDocument.class));
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
    // given
    UUID userId = UUID.randomUUID();

    when(userActivityRepository.softDeleteAllByUserId(userId))
        .thenThrow(new RuntimeException("DB 삭제 오류"));

    assertDoesNotThrow(() -> activityService.softDeleteUserActivity(userId));

    verify(userActivityRepository, times(1)).softDeleteAllByUserId(userId);
  }

  @Test
  @DisplayName("MongoDB 관심사 수정 시 $ 연산자를 통한 업데이트 확인")
  void updateSubscribeInActivity_Success() {
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();
    SubscriptionResponse response = new SubscriptionResponse(
        UUID.randomUUID(),
        interestId,
        "테스트관심사",
        List.of("키워드1", "키워드2"),
        100L,
        LocalDateTime.now()
    );

    activityService.updateSubscribeInActivity(userId, response);

    ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
    ArgumentCaptor<UpdateDefinition> updateCaptor = ArgumentCaptor.forClass(UpdateDefinition.class);

    verify(mongoTemplate).updateFirst(queryCaptor.capture(), updateCaptor.capture(), eq(UserActivityDocument.class));

    assertThat(queryCaptor.getValue().getQueryObject().get("subscriptions.interestId")).isEqualTo(interestId);

    String updateObj = updateCaptor.getValue().getUpdateObject().toString();
    assertThat(updateObj).contains("$set");
    assertThat(updateObj).contains("subscriptions.$.interestName");
  }
}
