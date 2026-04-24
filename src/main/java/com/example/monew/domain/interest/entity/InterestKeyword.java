package com.example.monew.domain.interest.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "interest_keywords")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterestKeyword {

  @Id
  @Column(columnDefinition = "uuid")
  private UUID id = UUID.randomUUID();

  @Column(name = "value", nullable = false)
  private String value;

  @ManyToOne
  @JoinColumn(name = "interest_id")
  private Interest interest;

  InterestKeyword(Interest interest, String value) {
    this.interest = Objects.requireNonNull(interest);
    this.value = Objects.requireNonNull(value);
  }
}
