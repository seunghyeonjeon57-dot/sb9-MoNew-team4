package com.example.monew.domain.comment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentEntity {

  @Id
  @GeneratedValue(generator = "uuid2")
  @GenericGenerator(name = "uuid2", strategy = "uuid2")
  private UUID id;

  @Column(name = "article_id", nullable = false)
  private UUID articleId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

   @Column(nullable = false, length = 500)
  private String content;

  @Column(name = "like_count")
  private Long likeCount = 0L;

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  public CommentEntity(UUID articleId, UUID userId, String content) {
    this.articleId = articleId;
    this.userId = userId;
    this.content = content;
    this.likeCount = 0L;
    this.createdAt = LocalDateTime.now();
  }


  public void updateContent(String content) {
    this.content = content;
  }

}