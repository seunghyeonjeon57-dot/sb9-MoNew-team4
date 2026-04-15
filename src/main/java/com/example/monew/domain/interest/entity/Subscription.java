package com.example.monew.domain.interest.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "subscriptions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription {

  @Id
  @Column(columnDefinition = "uuid")
  private UUID id = UUID.randomUUID();

  @Column(nullable = false)
  private UUID interestId;

  @Column(nullable = false)
  private UUID userId;

  public Subscription(UUID interestId, UUID userId) {
    this.interestId = Objects.requireNonNull(interestId, "interestId는 null일 수 없습니다.");
    this.userId = Objects.requireNonNull(userId, "userId는 null일 수 없습니다.");
  }
}
