package com.example.monew.domain.article.entity;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.example.monew.domain.user.entity.User;

import java.time.LocalDateTime;
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "article_views")
public class ArticleViewEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "article_id", nullable = false)
  private ArticleEntity articleEntity;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "viewed_by", nullable = false)
  private User viewedBy;

  // 중복 방지를 위한 IP 주소를 저장
  private String clientIp;

  @CreatedDate
  @Column(updatable = false)
  private LocalDateTime viewedAt;

  @Builder
  public ArticleViewEntity(ArticleEntity article, User viewedBy, String clientIp) {
    this.articleEntity = article;
    this.viewedBy = viewedBy;
    this.clientIp = clientIp;
  }
}