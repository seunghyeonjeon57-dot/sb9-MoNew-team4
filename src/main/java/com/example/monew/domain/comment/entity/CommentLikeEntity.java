package com.example.monew.domain.comment.entity;


import com.example.monew.global.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "comment_likes")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentLikeEntity  extends BaseEntity {

  @Id
  @GeneratedValue
  @Column(name = "id", columnDefinition = "UUID")
  private UUID id;

  @Column(name = "comment_id", nullable = false)
  private UUID commentId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  public CommentLikeEntity(UUID commentId, UUID userId) {
    this.commentId = commentId;
    this.userId = userId;
  }
}
