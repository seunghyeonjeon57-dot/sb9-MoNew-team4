package com.example.monew.domain.article.entity;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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

  @Column(nullable = false)
  private String viewerId;

  // 중복 방지를 위한 IP 주소를 저장
  private String clientIp;

  @CreatedDate
  @Column(updatable = false)
  private LocalDateTime viewedAt;

  @Builder
  public ArticleViewEntity(ArticleEntity article, String viewerId, String clientIp) {
    this.articleEntity = article;
    this.viewerId = viewerId;
    this.clientIp = clientIp;
  }
}