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
import com.example.monew.domain.interest.repository.InterestRepository;
import com.example.monew.domain.interest.repository.SubscriptionRepository;
import com.example.monew.domain.user.entity.User;
import com.example.monew.domain.user.exception.UserNotFoundException;
import com.example.monew.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RDBService {

  private final SubscriptionRepository subscriptionRepository;
  private final CommentRepository commentRepository;
  private final CommentLikeRepository commentLikeRepository;
  private final ArticleViewRepository articleViewRepository;
  private final ArticleRepository articleRepository;
  private final InterestRepository interestRepository;
  private final UserRepository userRepository;

  public UserActivityDto getUserActivity(UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

    return UserActivityDto.builder()
        .id(user.getId())
        .email(user.getEmail())
        .nickname(user.getNickname())
        .createdAt(user.getCreatedAt())
        .subscriptions(getSubscriptions(userId))
        .comments(getRecentComments(userId))
        .commentLikes(getRecentLikes(userId))
        .articleViews(getRecentArticles(userId))
        .build();
  }

  public List<SubscriptionResponse> getSubscriptions(UUID userId) {
    return subscriptionRepository.findAllByUserId(userId).stream()
        .map(sub -> SubscriptionResponse.of(sub,
            interestRepository.findById(sub.getInterestId())
                .orElseThrow()))
        .collect(Collectors.toList());
  }

  public List<CommentActivityDto> getRecentComments(UUID userId) {
    List<CommentEntity> comments = commentRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId);
    Set<UUID> articleIds = comments.stream().map(CommentEntity::getArticleId).collect(Collectors.toSet());
    Set<UUID> userIds = comments.stream().map(CommentEntity::getUserId).collect(Collectors.toSet());

    Map<UUID, ArticleEntity> articles = articleRepository.findAllById(articleIds).stream()
        .collect(Collectors.toMap(ArticleEntity::getId, a -> a));
    Map<UUID, User> users = userRepository.findAllById(userIds).stream()
        .collect(Collectors.toMap(User::getId, u -> u));

    return comments.stream()
        .map(comment -> CommentActivityDto.of(
            comment,
            articles.get(comment.getArticleId()),
            users.get(comment.getUserId())
        ))
        .collect(Collectors.toList());
  }

  public List<CommentLikeActivityDto> getRecentLikes(UUID userId) {
    List<CommentLikeEntity> likes = commentLikeRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId);

    Set<UUID> commentIds = likes.stream().map(CommentLikeEntity::getCommentId).collect(Collectors.toSet());
    Set<UUID> userIds = likes.stream().map(CommentLikeEntity::getUserId).collect(Collectors.toSet());

    Map<UUID, CommentEntity> comments = commentRepository.findAllById(commentIds).stream()
        .collect(Collectors.toMap(CommentEntity::getId, c -> c));

    Set<UUID> articleIds = comments.values().stream().map(CommentEntity::getArticleId).collect(Collectors.toSet());
    Map<UUID, ArticleEntity> articles = articleRepository.findAllById(articleIds).stream()
        .collect(Collectors.toMap(ArticleEntity::getId, a -> a));

    Map<UUID, User> users = userRepository.findAllById(userIds).stream()
        .collect(Collectors.toMap(User::getId, u -> u));

    return likes.stream()
        .map(like -> {
          CommentEntity comment = comments.get(like.getCommentId());
          return CommentLikeActivityDto.of(
              like,
              comment,
              articles.get(comment.getArticleId()),
              users.get(like.getUserId())
          );
        })
        .collect(Collectors.toList());
  }

  public List<ArticleViewDto> getRecentArticles(UUID userId) {
    List<ArticleViewEntity> views = articleViewRepository.findTop10ByViewedByIdOrderByViewedAtDesc(userId);

    return views.stream()
        .map(v -> ArticleViewDto.of(v, v.getArticleEntity()))
        .collect(Collectors.toList());
  }
}
