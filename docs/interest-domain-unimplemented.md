# Interest 도메인 미구현 항목 정리

> 작성일: 2026-04-24
> 기준 브랜치: `develop`
> 분류 기준:
> - 분류 A: 타 도메인/인프라 미완으로 지금 당장 연동 불가
> - 분류 B: 스펙 불일치 — 코드는 있지만 Swagger/schema와 어긋나 있음
> - 분류 C: 컨벤션 개선 — 동작은 정상이나 팀 관례와 불일치

---

## 분류 A — 타 도메인/인프라 미완으로 연동 불가

### A-1. MongoDB UserActivity 구독 연동

**요구사항**
기술 요구사항 T7: 사용자가 관심사를 구독하거나 취소할 때 MongoDB `UserActivityDocument`의 `subscribedInterests` 목록을 실시간 동기화해야 합니다.

**현재 상태**
- `InterestSubscriptionService.subscribe()` / `unsubscribe()`에서 PostgreSQL 구독 저장은 완료
- MongoDB `UserActivityDocument` 갱신 코드 없음

**막혀 있는 이유**
`ActivityService`에 `addSubscribedInterest()` / `removeSubscribedInterest()` 메서드가 아직 없습니다.

```java
// domain/activityManagement/service/ActivityService.java — 현재
// getUserActivity() 메서드만 존재, 구독 연동 메서드 미구현
```

**구현에 필요한 것**

ActivityService에 추가 필요:
```java
// 구독 등록 시 호출
public void addSubscribedInterest(UUID userId, InterestResponse interest) { ... }

// 구독 취소 시 호출
public void removeSubscribedInterest(UUID userId, UUID interestId) { ... }
```

InterestSubscriptionService 수정 위치:
```java
// InterestSubscriptionService.subscribe() 끝부분
interestRepository.incrementSubscriberCount(interest.getId());
activityService.addSubscribedInterest(userId, InterestResponse.from(interest, true)); // 추가
return SubscriptionResponse.of(saved, interest);

// InterestSubscriptionService.unsubscribe() 끝부분
interestRepository.decrementSubscriberCount(interestId);
activityService.removeSubscribedInterest(userId, interestId); // 추가
```

**주의사항**
PostgreSQL 트랜잭션 커밋 후 MongoDB 갱신이 따라가는 구조이므로, 커밋 직전 예외 발생 시 두 저장소가 불일치할 수 있습니다.
간단한 구현: 동기 직접 호출.
안전한 구현: `@TransactionalEventListener(phase = AFTER_COMMIT)`으로 PostgreSQL 커밋 이후에만 MongoDB 갱신.

**선행 조건**
ActivityService에 위 두 메서드가 먼저 구현되어야 연동 가능.

---

### A-2. Notification 알림 트리거 연동

**요구사항**
기능 요구사항: 구독한 관심사에 새 기사가 등록되면 구독자에게 알림을 생성해야 합니다.
알림 메시지 형식: `"[관심사명]와 관련된 기사가 N건 등록되었습니다."`

**현재 상태**
- `NotificationService.createNotification()` 단일 메서드만 존재 (1건 생성)
- Article 배치 → Interest 구독자 조회 → 알림 생성 흐름 미구현

**막혀 있는 이유**

두 가지 의존 도메인이 모두 미완입니다.

Article 배치 (article/batch/) 쪽:
- 기사 수집 후 해당 관심사 구독자 UUID 목록을 가져오는 로직 없음

Notification 도메인 (notification/) 쪽:
- 벌크 알림 생성 메서드 없음 (한 번에 N명에게 동일 알림 생성)
- NotificationService에 `notifyNewArticles()` 등 인터페이스 미정의

**구현에 필요한 것**

SubscriptionRepository에 추가 필요:
```java
// 해당 관심사 구독자 UUID 목록 조회 — Article 배치가 호출
@Query("SELECT s.userId FROM Subscription s WHERE s.interestId = :interestId AND s.deletedAt IS NULL")
List<UUID> findUserIdsByInterestId(@Param("interestId") UUID interestId);
```

NotificationService에 추가 필요:
```java
// 관심사 신규 기사 알림 벌크 생성
public void notifyNewArticles(UUID interestId, String interestName,
                               int articleCount, List<UUID> subscriberIds) {
    String message = String.format("[%s]와 관련된 기사가 %d건 등록되었습니다.",
                                   interestName, articleCount);
    for (UUID userId : subscriberIds) {
        createNotification(userId, message, ResourceType.INTEREST, interestId);
    }
}
```

Article 배치 연동 위치:
```
NewsCollector (or ArticleService)
  → 기사 저장 완료 후
  → subscriptionRepository.findUserIdsByInterestId(interest.getId())
  → notificationService.notifyNewArticles(...)
```

**선행 조건**
- Notification 도메인 컨트롤러/DTO/커서 페이지네이션/읽음 처리 구현 (MON-80 계열)
- Article 배치와 Notification 서비스 연결 지점 팀 합의

---

### A-3. User 삭제 시 Subscription cascade 트리거

**요구사항**
User가 탈퇴하면 해당 사용자의 구독 레코드를 모두 삭제하고, 각 구독 관심사의 `subscriberCount`를 감소시켜야 합니다.

**현재 상태**
`SubscriptionRepository`에 `deleteAllByUserId(UUID userId)` 메서드 구현됨.

```java
// SubscriptionRepository.java:35-38
@Modifying
@Transactional
@Query("DELETE FROM Subscription s WHERE s.userId = :userId")
long deleteAllByUserId(@Param("userId") UUID userId);
```

단, 이 메서드는 구독 레코드만 삭제하고 `subscriberCount` 감소는 하지 않습니다.
`InterestRepository`에는 `decrementSubscriberCountAll()` 메서드도 있지만 아직 User 삭제 흐름에서 사용하지 않습니다.

**막혀 있는 이유**
User 도메인의 회원 탈퇴 서비스가 아직 완성되지 않아, 탈퇴 처리 흐름에서 Interest 측 메서드를 호출하는 지점이 없습니다.

**구현에 필요한 것**

UserService.delete() (또는 탈퇴 처리 메서드) 내부에서:
```java
// 1. 구독 중인 관심사 ID 목록 조회
List<UUID> subscribedInterestIds = subscriptionRepository.findInterestIdsByUserId(userId);

// 2. 각 관심사 구독자 수 일괄 감소
interestRepository.decrementSubscriberCountAll(subscribedInterestIds);

// 3. 구독 레코드 일괄 삭제
subscriptionRepository.deleteAllByUserId(userId);
```

**선행 조건**
User 도메인 탈퇴 API 구현 완료 필요.

---

## 분류 B — 스펙 불일치 기술 부채

### B-1. InterestKeyword 컬럼명 불일치

**파일 및 위치**
- `entity/InterestKeyword.java` 25행: `@Column(name = "keyword_value")`
- `src/main/resources/schema.sql` 32행: `value VARCHAR(50) NOT NULL`

**현재 동작**
- `test` 프로파일: `ddl-auto: create`이므로 Hibernate가 엔티티 기준으로 테이블 생성 → `keyword_value` 컬럼으로 생성, 테스트 통과
- `dev` 프로파일에서 schema.sql로 DB를 초기화하면 `value` 컬럼으로 생성 → 엔티티의 `keyword_value` 참조와 불일치

**수정**

InterestKeyword.java 25행:
```java
// 현재
@Column(name = "keyword_value", nullable = false)

// 수정
@Column(name = "value", nullable = false)
```

수정 후 `./gradlew clean test`로 전체 테스트 재확인 필요.

---

### B-2. Swagger 스펙에 409 응답 코드 누락 (MON-146)

**파일 및 위치**
- `docs/monew-swagger.json`
- POST `/api/interests/{interestId}/subscriptions` 응답 코드 목록

**현재 상태**
- Swagger 스펙: 200, 404, 500만 명시
- 실제 구현: 중복 구독 시 409 DUPLICATE_SUBSCRIPTION 반환
- 테스트 주석에 알려진 이슈로 기록됨 (`InterestSubscriptionServiceTest.java` 121~125행)

**수정**

docs/monew-swagger.json의 해당 endpoint responses에 추가:
```json
"409": {
  "description": "이미 구독 중인 관심사",
  "content": {
    "application/json": {
      "schema": { "$ref": "#/components/schemas/ErrorResponse" }
    }
  }
}
```

---

### B-3. Interest/Subscription 삭제 방식 불명확

**현재 상태**
- `Interest`와 `Subscription` 엔티티 모두 `BaseEntity`에서 `deletedAt` 필드를 상속
- 실제 삭제 처리는 `interestRepository.delete()`와 `subscriptionRepository.deleteAllByInterestId()` — 물리 삭제
- Swagger operationId가 `hardDelete`이므로 물리 삭제가 스펙에 맞음

**CLAUDE.md와의 관계**
프로젝트 CLAUDE.md는 "삭제는 원칙적으로 soft delete"를 가이드하지만, 현재 Interest 도메인 스펙 자체가 물리 삭제입니다. 다른 도메인(comment 등)에서 soft delete가 필요한 경우 참고할 수 있도록 기록합니다.

**현재 방식을 유지하는 이유**
- Swagger 스펙 operationId: `hardDelete` → 의도된 물리 삭제
- 관심사 삭제 후 해당 관심사 관련 데이터(구독, 관심사 기반 알림 등)도 함께 정리하는 구조가 단순해짐

**추후 soft delete로 전환이 필요한 경우**
`InterestService.delete()` 수정:
```java
// 현재 (물리 삭제)
subscriptionRepository.deleteAllByInterestId(interestId);
interestRepository.delete(interest);

// soft delete 전환 시
interest.markDeleted();               // BaseEntity.markDeleted() 호출
interestRepository.save(interest);
```

---

## 분류 C — 컨벤션 개선 항목

### C-1. buildCursorWhere() 조건 중첩 깊이 3단계

**파일 및 위치**
- `InterestRepositoryImpl.java` 75~93행

**현재 코드 구조**
```
buildCursorWhere()
  if (cursorId != null)          — depth 1
    if (anchor != null)          — depth 2
      return fullKeyset()
    if (after != null)           — depth 2 (else 분기)
      return timestampKeyset()
```

팀 컨벤션: if 문 depth 2단계 이하.

**리팩터링 방향**
```java
private BooleanExpression buildCursorWhere(
    String orderBy, String direction, UUID cursorId, LocalDateTime after) {
    if (cursorId == null && after == null) return null;
    if (cursorId == null) return interest.createdAt.gt(after);

    Interest anchor = queryFactory.selectFrom(interest)
        .where(interest.id.eq(cursorId).and(interest.deletedAt.isNull()))
        .fetchOne();
    if (anchor != null) return fullKeyset(orderBy, direction, anchor);
    if (after != null)  return timestampKeyset(after, cursorId);
    return null;
}
```

로직 동일, early return 패턴으로 depth 1단계 감소.

---

### C-2. SubscriptionRepository 인터페이스에 @Transactional 선언

**파일 및 위치**
- `SubscriptionRepository.java` 32행, 36행

**현재 코드**
```java
@Transactional
long deleteAllByInterestId(UUID interestId);

@Modifying
@Transactional
@Query("DELETE FROM Subscription s WHERE s.userId = :userId")
long deleteAllByUserId(@Param("userId") UUID userId);
```

**문제**
팀 컨벤션: 트랜잭션 경계는 서비스 레이어에서 관리. 리포지토리 인터페이스에 `@Transactional`을 선언하면 서비스 트랜잭션 밖에서 호출했을 때 의도치 않게 별도 트랜잭션으로 실행될 수 있습니다.

**수정 방향**
리포지토리의 `@Transactional` 제거. 호출하는 서비스 메서드(`InterestService.delete()`, 추후 `UserService.delete()`)에 이미 `@Transactional`이 있으므로 동작은 동일합니다.

`@Modifying`만 남기면 됩니다. (`@Modifying`은 JPQL 쓰기 쿼리에 필수)

---

### C-3. Interest 엔티티 Builder에서 IllegalArgumentException 직접 throw

**파일 및 위치**
- `Interest.java` 41행, 44행

**현재 코드**
```java
@Builder
public Interest(String name, List<String> keywords) {
    if (!StringUtils.hasText(name)) {
        throw new IllegalArgumentException("관심사 이름은 비어 있을 수 없습니다.");
    }
    if (keywords == null || keywords.isEmpty()) {
        throw new IllegalArgumentException("키워드는 최소 1개 이상이어야 합니다.");
    }
    ...
}
```

**현재 동작에 문제 없는 이유**
컨트롤러의 `@Valid`가 먼저 차단하므로 엔티티 Builder까지 잘못된 값이 도달하지 않습니다.
설령 도달하더라도 `GlobalException`의 `handleIllegalArgumentException()` 핸들러가 400으로 처리합니다.

**개선이 필요한 이유**
배치 작업이나 테스트에서 엔티티를 직접 생성할 때 `MonewException` 계약과 다른 예외가 발생합니다.
팀의 예외 처리 계층(MonewException → ErrorCode)을 우회합니다.

**수정 방향**
```java
// Interest.java
@Builder
public Interest(String name, List<String> keywords) {
    if (!StringUtils.hasText(name)) {
        throw new InterestCreateException(Map.of("reason", "name is blank"));
    }
    if (keywords == null || keywords.isEmpty()) {
        throw new InterestCreateException(Map.of("reason", "keywords is empty"));
    }
    ...
}
```

단, 엔티티에서 도메인 예외를 import하면 엔티티→서비스 방향 의존이 생길 수 있습니다. 팀에서 허용 여부 합의 후 적용.

---

## 우선순위 요약

**즉시 수정 권장 (B 분류)**

- B-1 InterestKeyword 컬럼명 `keyword_value` → `value` 수정
  - dev 환경 schema.sql 기반 초기화 시 버그 발생 가능
  - 수정 난이도 낮음, 테스트 영향 없음

- B-2 Swagger 409 응답 추가 (MON-146)
  - swagger.json 수정만으로 해결
  - 클라이언트 기대값 일치

**타 도메인 구현 후 연동 (A 분류)**

- A-1 UserActivity 구독 연동: ActivityService 메서드 추가 후 InterestSubscriptionService에 2줄 추가
- A-2 Notification 알림 트리거: Notification 도메인 완성 후 Article 배치와 함께 연동
- A-3 User 탈퇴 cascade: UserService 탈퇴 메서드 완성 후 연동

**리팩터링 시 정리 (C 분류)**

- C-1 buildCursorWhere() early return 리팩터링
- C-2 SubscriptionRepository @Transactional 제거
- C-3 Interest 생성자 예외 처리 개선
