package com.example.monew.domain.article.entity;

import com.example.monew.global.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;


@Entity
@Getter
@Table(name = "articles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE articles SET is_deleted = true WHERE article_id = ?")
@Where(clause = "is_deleted = false")
//@Where(clause = "deleted_at IS NULL")
//@SQLDelete(sql = "UPDATE articles SET deleted_at = CURRENT_TIMESTAMP WHERE article_id = ?") //선우님한테 말해서 이걸로 대체 가능한지
public class ArticleEntity extends BaseEntity {

  @Id
  @GeneratedValue
  @Column(name = "article_id", columnDefinition = "UUID")
  private UUID id;

  @Column(nullable = false, length = 100)
  private String source;

  @Column(name = "source_url", nullable = false, unique = true, length = 500)
  private String sourceUrl;

  @Column(nullable = false, length = 300)
  private String title;

  @Column(name = "publish_date")
  private LocalDateTime publishDate;

  @Column(columnDefinition = "TEXT")
  private String summary;

  @Column(name = "comment_count", nullable = false)
  private long commentCount;

  @Column(name = "view_count", nullable = false)
  private long viewCount;

  private String interest;

  @Builder
  public ArticleEntity(UUID id, String source, String sourceUrl, String title,
      LocalDateTime publishDate, String summary, String interest) {
    this.id = id; // 생성자 매개변수에 id 추가 및 할당
    this.source = source;
    this.sourceUrl = sourceUrl;
    this.title = title;
    this.publishDate = publishDate;
    this.summary = summary;
    this.interest = interest;
    this.commentCount = 0;
    this.viewCount = 0;
  }

  // 동일 사용자 체크 후 연산자로 1 증가
  public void incrementViewCount() {
    this.viewCount++;
  }
}