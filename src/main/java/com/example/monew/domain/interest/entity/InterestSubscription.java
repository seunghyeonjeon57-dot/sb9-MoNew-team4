package com.example.monew.domain.interest.entity;

import com.example.monew.global.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "interest_subscription",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_interest_subscription_interest_user",
        columnNames = {"interest_id", "user_id"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterestSubscription extends BaseEntity {

  @Id
  @Column(name = "id", columnDefinition = "uuid")
  private UUID id;

  @Column(name = "interest_id", nullable = false, columnDefinition = "uuid")
  private UUID interestId;

  @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
  private UUID userId;

  public InterestSubscription(UUID interestId, UUID userId) {
    this.id = UUID.randomUUID();
    this.interestId = interestId;
    this.userId = userId;
  }
}
