package com.example.monew.domain.comment.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "comment_likes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentLikeEntity {

  @Id
  @GeneratedValue
  @Column(name = "commentLike_id", columnDefinition = "UUID")
  private UUID id;

  @Column(name = "comment_id", nullable = false)
  private UUID commentId;

  @Column(name = "user_Id", nullable = false)
  private UUID userId;

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  public CommentLikeEntity(UUID commentId, UUID userId) {
    this.commentId = commentId;
    this.userId = userId;
    this.createdAt = LocalDateTime.now();
  }
}
