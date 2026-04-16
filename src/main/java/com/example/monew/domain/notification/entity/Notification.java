package com.example.monew.domain.notification.entity;

import com.example.monew.global.base.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull(message = "사용자 ID는 필수입니다.")
  @Column(name = "user_id", nullable = false)
  private Long userId;

  @NotBlank(message = "알림 내용은 필수입니다.")
  @Column(nullable = false)
  private String content;

  @Enumerated(EnumType.STRING)
  @NotNull(message = "관련 리소스 타입은 필수입니다.")
  @Column(name = "resource_type", nullable = false)
  private ResourceType resourceType;

  @NotNull(message = "관련 리소스 ID는 필수입니다.")
  @Column(name = "resource_id", nullable = false)
  private Long resourceId;

  @Column(name = "is_read", nullable = false)
  private boolean isRead = false;

  @Builder
  public Notification(Long userId, String content, ResourceType resourceType, Long resourceId) {
    this.userId = userId;
    this.content = content;
    this.resourceType = resourceType;
    this.resourceId = resourceId;
    this.isRead = false; // 알림 생성 시점에는 항상 안 읽은 상태
  }

  // 알림 읽음 처리 비즈니스 로직
  public void read() {
    this.isRead = true;
  }
}