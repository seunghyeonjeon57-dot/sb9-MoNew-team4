package com.example.monew.domain.interest.entity;

import com.example.monew.global.base.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "interest",
    uniqueConstraints = @UniqueConstraint(name = "uk_interest_name", columnNames = "name")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Interest extends BaseEntity {

  @Id
  @Column(name = "id", columnDefinition = "uuid")
  private UUID id;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(
      name = "interest_keyword",
      joinColumns = @JoinColumn(name = "interest_id")
  )
  @Column(name = "keyword", nullable = false, length = 100)
  private List<String> keywords = new ArrayList<>();

  @Column(name = "subscriber_count", nullable = false)
  private long subscriberCount = 0L;

  public Interest(String name, List<String> keywords) {
    this.id = UUID.randomUUID();
    this.name = name;
    this.keywords = new ArrayList<>(keywords);
    this.subscriberCount = 0L;
  }

  public void replaceKeywords(List<String> newKeywords) {
    this.keywords.clear();
    this.keywords.addAll(newKeywords);
  }

  public void increaseSubscriberCount() {
    this.subscriberCount++;
  }

  public void decreaseSubscriberCount() {
    if (this.subscriberCount > 0) {
      this.subscriberCount--;
    }
  }
}
