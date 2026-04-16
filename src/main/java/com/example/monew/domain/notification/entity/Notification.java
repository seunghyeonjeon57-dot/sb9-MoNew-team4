package com.example.monew.domain.notification.entity;

import com.example.monew.global.base.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notifications")
public class Notification extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID) // UUID 생성 전략
  private UUID id;

  @NotNull(message = "사용자 ID는 필수입니다.")
  @Column(name = "user_id", nullable = false)
  private UUID userId; // Long -> UUID

  @NotNull(message = "알림 내용은 필수입니다.")
  @Column(nullable = false)
  private String content;

  @NotNull(message = "리소스 타입은 필수입니다.")
  @Enumerated(EnumType.STRING)
  @Column(name = "resource_type", nullable = false)
  private ResourceType resourceType;

  @NotNull(message = "관련 리소스 ID는 필수입니다.")
  @Column(name = "resource_id", nullable = false)
  private UUID resourceId; // Long -> UUID

  @Column(name = "is_read", nullable = false)
  private boolean isRead = false;

  @Builder
  public Notification(UUID userId, String content, ResourceType resourceType, UUID resourceId) {
    this.userId = userId;
    this.content = content;
    this.resourceType = resourceType;
    this.resourceId = resourceId;
    this.isRead = false;
  }

  public void read() {
    this.isRead = true;
  }
}