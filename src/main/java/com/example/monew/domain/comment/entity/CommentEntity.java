package com.example.monew.domain.comment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
import com.example.monew.global.base.BaseEntity;


@Entity
@Table(name = "comments")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentEntity extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "article_id", nullable = false)
  private UUID articleId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "content",nullable = false, length = 500)
  private String content;

  @Builder.Default
  @Column(name = "like_count")
  private Long likeCount = 0L;


  public CommentEntity(UUID articleId, UUID userId, String content) {
    this.articleId = articleId;
    this.userId = userId;
    this.content = content;
    this.likeCount = 0L;
  }


  public void updateContent(String content) {
    this.content = content;
  }


  public void incrementLikeCount() {
    if (this.likeCount == null) {
      this.likeCount = 0L;
    }
    this.likeCount++;
  }

  public void decrementLikeCount() {
    if (this.likeCount == null || likeCount <= 0) {
      this.likeCount = 0L;
      return;
    }
    this.likeCount--;
  }
}
