package com.example.monew.domain.activityManagement.document;


import com.example.monew.domain.article.dto.ArticleViewDto;
import com.example.monew.domain.activityManagement.dto.CommentActivityDto;
import com.example.monew.domain.activityManagement.dto.CommentLikeActivityDto;
import com.example.monew.domain.interest.dto.InterestResponse;
import com.example.monew.domain.interest.dto.SubscriptionResponse;
import com.example.monew.domain.user.dto.UserDto;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "user_activities")
@Getter
@Builder
public class UserActivityDocument {
  @Id
  @Field("_id")
  private UUID userId;

  private UserDto userProfile;

  @Builder.Default
  private List<SubscriptionResponse> subscribedInterests = new ArrayList<>();
  @Builder.Default
  private List<CommentActivityDto> recentComments = new ArrayList<>();
  @Builder.Default
  private List<CommentLikeActivityDto> recentLikes = new ArrayList<>();
  @Builder.Default
  private List<ArticleViewDto> recentArticles = new ArrayList<>();

  private LocalDateTime updatedAt;
}
