package com.example.monew.domain.activity.service;

import com.example.monew.domain.activity.dto.CommentActivityDto;
import com.example.monew.domain.activity.dto.CommentLikeActivityDto;
import com.example.monew.domain.activity.dto.UserActivityDto;
import com.example.monew.domain.article.dto.ArticleViewDto;
import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.article.entity.ArticleViewEntity;
import com.example.monew.domain.article.repository.ArticleRepository;
import com.example.monew.domain.article.repository.ArticleViewRepository;
import com.example.monew.domain.comment.entity.CommentEntity;
import com.example.monew.domain.comment.entity.CommentLikeEntity;
import com.example.monew.domain.comment.repository.CommentLikeRepository;
import com.example.monew.domain.comment.repository.CommentRepository;
import com.example.monew.domain.interest.dto.SubscriptionResponse;
import com.example.monew.domain.interest.entity.Interest;
import com.example.monew.domain.interest.entity.Subscription;
import com.example.monew.domain.interest.repository.InterestRepository;
import com.example.monew.domain.interest.repository.SubscriptionRepository;
import com.example.monew.domain.user.entity.User;
import com.example.monew.domain.user.exception.UserNotFoundException;
import com.example.monew.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class RdbServiceTest {

  @Mock
  private SubscriptionRepository subscriptionRepository;
  @Mock
  private CommentRepository commentRepository;
  @Mock
  private CommentLikeRepository commentLikeRepository;
  @Mock
  private ArticleViewRepository articleViewRepository;
  @Mock
  private ArticleRepository articleRepository;
  @Mock
  private InterestRepository interestRepository;
  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private RDBService rdbService;

  @Test
  @DisplayName("성공: 사용자 정보와 모든 활동 내역(구독, 댓글, 좋아요, 조회)을 통합하여 반환한다")
  void getUserActivity_Success() {
    UUID userId = UUID.randomUUID();

    User mockUser = User.builder()
        .email("tester@example.com")
        .nickname("테스터")
        .build();
    ReflectionTestUtils.setField(mockUser, "id", userId);
    ReflectionTestUtils.setField(mockUser, "createdAt", LocalDateTime.now());

    List<SubscriptionResponse> mockSubs = List.of(mock(SubscriptionResponse.class));
    List<CommentActivityDto> mockComments = List.of(mock(CommentActivityDto.class));
    List<CommentLikeActivityDto> mockLikes = List.of(mock(CommentLikeActivityDto.class));
    List<ArticleViewDto> mockViews = List.of(mock(ArticleViewDto.class));

    given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));


    UserActivityDto result = rdbService.getUserActivity(userId);

    assertThat(result).isNotNull();
    assertThat(result.id()).isEqualTo(userId);
    assertThat(result.email()).isEqualTo("tester@example.com");
    assertThat(result.nickname()).isEqualTo("테스터");

    assertThat(result.subscriptions()).isNotNull();
    assertThat(result.comments()).isNotNull();
    assertThat(result.commentLikes()).isNotNull();
    assertThat(result.articleViews()).isNotNull();

    verify(userRepository).findById(userId);
  }

  @Test
  @DisplayName("실패: 존재하지 않는 사용자를 조회하면 UserNotFoundException이 발생한다")
  void getUserActivity_UserNotFound() {
    UUID userId = UUID.randomUUID();
    given(userRepository.findById(userId)).willReturn(Optional.empty());

    assertThatThrownBy(() -> rdbService.getUserActivity(userId))
        .isInstanceOf(UserNotFoundException.class)
        .hasMessageContaining("찾을 수 없습니다.");
  }

  @Test
  @DisplayName("성공: 사용자가 구독 중인 모든 관심사 정보를 조회하여 DTO 리스트로 반환한다")
  void getSubscriptions_Success() {
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();

    Subscription mockSub = mock(Subscription.class);
    given(mockSub.getInterestId()).willReturn(interestId);

    Interest mockInterest = mock(Interest.class);
    given(mockInterest.getId()).willReturn(interestId);
    given(mockInterest.getName()).willReturn("IT/과학");
    given(mockInterest.getKeywords()).willReturn(List.of());

    given(subscriptionRepository.findAllByUserId(userId))
        .willReturn(List.of(mockSub));
    given(interestRepository.findById(interestId))
        .willReturn(Optional.of(mockInterest));

    List<SubscriptionResponse> result = rdbService.getSubscriptions(userId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).interestName()).isEqualTo("IT/과학");
    assertThat(result.get(0).interestId()).isEqualTo(interestId);

    verify(subscriptionRepository).findAllByUserId(userId);
    verify(interestRepository).findById(interestId);
  }

  @Test
  @DisplayName("성공: 사용자가 작성한 최신 댓글과 연관 기사, 유저 정보를 조회하여 DTO로 변환한다")
  void getRecentComments_Success() {
    UUID userId = UUID.randomUUID();
    UUID articleId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();

    CommentEntity mockComment = mock(CommentEntity.class);
    given(mockComment.getId()).willReturn(commentId);
    given(mockComment.getUserId()).willReturn(userId);
    given(mockComment.getArticleId()).willReturn(articleId);
    given(mockComment.getContent()).willReturn("정말 유익한 기사네요!");
    given(mockComment.getLikeCount()).willReturn(10L);
    given(mockComment.getCreatedAt()).willReturn(LocalDateTime.now());

    ArticleEntity mockArticle = mock(ArticleEntity.class);
    given(mockArticle.getId()).willReturn(articleId);
    given(mockArticle.getTitle()).willReturn("2026년 경제 전망");

    User mockUser = mock(User.class);
    given(mockUser.getId()).willReturn(userId);
    given(mockUser.getNickname()).willReturn("경제마스터");

    given(commentRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId))
        .willReturn(List.of(mockComment));

    given(articleRepository.findAllById(any())).willReturn(List.of(mockArticle));
    given(userRepository.findAllById(any())).willReturn(List.of(mockUser));

    List<CommentActivityDto> result = rdbService.getRecentComments(userId);

    assertThat(result).hasSize(1);
    CommentActivityDto dto = result.get(0);

    assertThat(dto.id()).isEqualTo(commentId);
    assertThat(dto.content()).isEqualTo("정말 유익한 기사네요!");
    assertThat(dto.articleTitle()).isEqualTo("2026년 경제 전망");
    assertThat(dto.userNickname()).isEqualTo("경제마스터");
    assertThat(dto.likeCount()).isEqualTo(10L);

    verify(commentRepository).findTop10ByUserIdOrderByCreatedAtDesc(userId);
    verify(articleRepository).findAllById(any());
    verify(userRepository).findAllById(any());
  }

  @Test
  @DisplayName("성공: 사용자가 좋아요를 누른 최근 내역과 연관 댓글, 기사, 유저 정보를 조회하여 DTO로 반환한다")
  void getRecentLikes_Success() {
    UUID userId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();
    UUID articleId = UUID.randomUUID();
    UUID likeId = UUID.randomUUID();

    CommentLikeEntity mockLike = mock(CommentLikeEntity.class);
    given(mockLike.getId()).willReturn(likeId);
    given(mockLike.getUserId()).willReturn(userId);
    given(mockLike.getCommentId()).willReturn(commentId);
    given(mockLike.getCreatedAt()).willReturn(LocalDateTime.now());

    User mockUser = mock(User.class);
    given(mockUser.getId()).willReturn(userId);
    given(mockUser.getNickname()).willReturn("좋아요");

    CommentEntity mockComment = mock(CommentEntity.class);
    given(mockComment.getId()).willReturn(commentId);
    given(mockComment.getArticleId()).willReturn(articleId);

    ArticleEntity mockArticle = mock(ArticleEntity.class);
    given(mockArticle.getId()).willReturn(articleId);

    given(commentLikeRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId))
        .willReturn(List.of(mockLike));
    given(commentRepository.findAllById(any())).willReturn(List.of(mockComment));
    given(articleRepository.findAllById(any())).willReturn(List.of(mockArticle));

    given(userRepository.findAllById(any())).willReturn(List.of(mockUser));

    List<CommentLikeActivityDto> result = rdbService.getRecentLikes(userId);

    assertThat(result).hasSize(1);

    CommentLikeActivityDto dto = result.get(0);

    assertThat(dto.id()).isEqualTo(likeId);
    assertThat(dto.commentUserNickname()).isEqualTo("좋아요");
  }

  @Test
  @DisplayName("성공: 사용자가 최근에 본 기사 10개를 조회하여 DTO로 반환한다")
  void getRecentArticles_Success() {
    UUID userId = UUID.randomUUID();
    UUID articleId = UUID.randomUUID();

    ArticleEntity mockArticle = mock(ArticleEntity.class);
    given(mockArticle.getId()).willReturn(articleId);
    given(mockArticle.getTitle()).willReturn("최근 본 뉴스 제목");

    ArticleViewEntity mockView = mock(ArticleViewEntity.class);
    User mockUser = mock(User.class);
    given(mockUser.getId()).willReturn(userId);

    given(mockView.getArticleEntity()).willReturn(mockArticle);
    given(mockView.getViewedBy()).willReturn(mockUser);
    given(mockView.getCreatedAt()).willReturn(LocalDateTime.now());

    given(articleViewRepository.findTop10ByViewedByIdOrderByViewedAtDesc(userId))
        .willReturn(List.of(mockView));

    List<ArticleViewDto> result = rdbService.getRecentArticles(userId);

    assertThat(result).hasSize(1);
    ArticleViewDto dto = result.get(0);

    assertThat(dto.getArticleId()).isEqualTo(articleId);
    assertThat(dto.getArticleTitle()).isEqualTo("최근 본 뉴스 제목");

    verify(articleViewRepository).findTop10ByViewedByIdOrderByViewedAtDesc(userId);
  }
}