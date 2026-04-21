# Interest 도메인 코드 리뷰 & 미구현 항목 구현 가이드

> 작성일: 2026-04-16  
> 기준 브랜치: `develop`  
> 작성자: 이종호 (MON-26)  
> 목적: Interest 도메인 구현 완료 후 코드 리뷰 결과와 후속 작업 가이드를 팀에 공유

---

## 1. 구현 완료 현황 (I1~I6)

기능 요구사항 I1~I6 전체가 구현 완료되었으며, **68개 테스트 전체 통과** 상태입니다.

| 요구사항 | HTTP | 엔드포인트 | 상태 | 핵심 구현 포인트 |
|---------|------|-----------|------|----------------|
| I1 관심사 등록 | POST | `/api/interests` | ✅ | `SimilarityUtils` Levenshtein 80% 유사도 차단, `@Valid`, 201 반환 |
| I2 목록 조회 | GET | `/api/interests` | ✅ | keyword/sortBy/direction/cursor/size 파라미터, `CursorPageResponse<T>` 반환 |
| I3 키워드 수정 | PATCH | `/api/interests/{id}` | ✅ | `@Valid`, soft delete 필터, 200 반환 |
| I4 관심사 삭제 | DELETE | `/api/interests/{id}` | ✅ | `markDeleted()`, 구독 cascade 삭제, 204 반환 |
| I5 구독 | POST | `/api/interests/{id}/subscriptions` | ✅ | DB unique constraint 기반 중복 방지, 원자적 카운터 증감, 201 반환 |
| I6 구독취소 | DELETE | `/api/interests/{id}/subscriptions` | ✅ | 원자적 카운터 감소, 204 반환 |

### 테스트 계층 구성 (68개)

| 계층 | 파일 | 테스트 수 | 검증 내용 |
|------|------|---------|---------|
| Unit (Service) | `InterestServiceTest` | 18 | 유사도 차단, 키워드 필터, 정렬, 커서 페이지네이션 |
| Unit (Subscription) | `InterestSubscriptionServiceTest` | 6 | 구독 생성/취소, 중복 방지, 카운터 |
| JPA Slice | `InterestRepositoryTest` | 3 | soft delete 필터 쿼리 |
| JPA Slice | `SubscriptionRepositoryTest` | 4 | 중복 체크, 일괄 삭제 |
| Web Slice | `InterestControllerTest` | 20 | 응답 구조, 상태코드, 예외 매핑 |
| Web Slice | `InterestSubscriptionControllerTest` | 9 | 구독/취소 API, 헤더 누락, 중복 |
| Entity | `InterestTest` | 5 | 엔티티 생성, 키워드 교체, 논리 삭제 |
| Entity | `SubscriptionTest` | 2 | 엔티티 생성, null ID 거부 |
| Exception | `InterestExceptionsTest` | 6 | ErrorCode 매핑, details 검증 |
| Integration | `InterestApiIntegrationTest` | 4 | 전체 플로우, 80% 유사도 정책, 에러 시나리오 |

### 핵심 설계 결정

- **커서 방식**: 마지막 아이템 UUID string (null/빈 문자열 → 첫 페이지). 이름은 정렬 기준에 따라 중복 가능하므로 UUID 사용.
- **in-memory 필터/정렬**: 관심사 수가 제한적이므로 DB 쿼리 없이 메모리 처리. 수가 크게 늘어날 경우 DB 쿼리로 전환 고려.
- **N+1 방지**: `subscribedByMe` 처리 시 `subscriptionRepository.findInterestIdsByUserIdAndInterestIdIn()` 단일 벌크 IN 쿼리 사용.
- **원자적 카운터**: `subscriberCount` 증감은 JPQL `UPDATE` 쿼리로 원자적 처리 (낙관적 락 없이 동시성 안전).

---

## 2. 컨벤션 위반 사항

### P2 — 컨트롤러 2개 분리 (팀 컨벤션 논의 필요)

**파일**: `InterestController.java` + `InterestSubscriptionController.java`  
**컨벤션 출처**: `docs/monew-team-convention.md` §2.2 — *"도메인 하나에 컨트롤러 하나"*

구독 엔드포인트(`/api/interests/{id}/subscriptions`)가 별도 컨트롤러로 분리되어 있습니다.  
중첩 URL 구조상 분리가 실용적이지만 팀 컨벤션 위반입니다.

**선택지**:

| 방안 | 장점 | 단점 |
|------|------|------|
| `InterestController`에 통합 | 컨벤션 준수 | `@RequestMapping` 충돌 → 메서드별 전체 경로 명시 필요 |
| 현행 유지 (2개 분리) | 코드 구조 명확 | 컨벤션 위반 |

팀에서 합의 후 통일 필요.

---

### P3 — `InterestSubscriptionController` 필드명 `service`

**파일**: `InterestSubscriptionController.java:23`  
**컨벤션**: 역할 접미사 필수

```java
// 현재 (위반 — 역할 접미사 없음)
private final InterestSubscriptionService service;

// 수정 후
private final InterestSubscriptionService interestSubscriptionService;
```

수정 시 동일 파일 내 `service.subscribe(...)`, `service.unsubscribe(...)` 참조도 함께 변경.

---

### P3 — `Interest` 엔티티 생성자의 `IllegalArgumentException` 직접 사용

**파일**: `Interest.java:38-43`

```java
// 현재 — MonewException 패턴 우회
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

**현재 동작**: `@Valid` Bean Validation이 컨트롤러에서 먼저 차단하므로 엔티티 생성자까지 도달하지 않음. 따라서 기능 동작에는 이상 없음.  
**잠재적 문제**: 배치 작업이나 테스트에서 엔티티를 직접 생성할 때 `IllegalArgumentException`이 `GlobalException`의 400 핸들러로 처리되지만, 팀의 `MonewException` 패턴과 불일치.  
**권장**: 기능 우선 개발 이후 리팩터링 시 정리.

---

### P3 — `updateKeywords` 응답의 `subscribedByMe` 하드코딩

**파일**: `InterestService.java:137`

```java
// 현재 — 항상 false 반환
return InterestResponse.from(interest, false);
```

**문제**: 키워드 수정 후 응답에서 구독 중인 사용자도 `subscribedByMe = false`를 받음.  
**수정 방법**:

```java
// InterestController.updateKeywords() — userId 서비스로 전달
@PatchMapping("/{id}")
public ResponseEntity<InterestResponse> updateKeywords(
    @RequestHeader("Monew-Request-User-ID") UUID userId,
    @PathVariable UUID id,
    @Valid @RequestBody InterestUpdateRequest request) {
  return ResponseEntity.ok(interestService.updateKeywords(id, request, userId)); // userId 추가
}

// InterestService.updateKeywords() — userId 파라미터 추가
@Transactional
public InterestResponse updateKeywords(UUID interestId, InterestUpdateRequest request, UUID userId) {
    Interest interest = interestRepository.findByIdAndDeletedAtIsNull(interestId)
        .orElseThrow(() -> new InterestNotFoundException(Map.of("interestId", interestId.toString())));
    interest.replaceKeywords(request.keywords());
    boolean subscribed = userId != null &&
        subscriptionRepository.existsByInterestIdAndUserId(interestId, userId);
    return InterestResponse.from(interest, subscribed);
}
```

**영향 범위**: `InterestController`, `InterestService`, `InterestServiceTest`, `InterestControllerTest`

---

### P3 — Swagger/OpenAPI 어노테이션 없음

**파일**: `InterestController.java`, `InterestSubscriptionController.java`  
**현황**: 모든 엔드포인트에 `@Operation`, `@ApiResponse`, `@Parameter` 없음  
**영향**: Swagger UI(`/swagger-ui/index.html`)에서 API 설명·파라미터·응답코드 정보 없음

구현 예시 (`InterestController`):

```java
@Operation(summary = "관심사 목록 조회", description = "키워드 부분일치 검색, 정렬(name/subscriberCount), 커서 페이지네이션 지원")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(responseCode = "400", description = "정렬 파라미터 오류 (INVALID_SORT_PARAMETER)")
})
@GetMapping
public ResponseEntity<CursorPageResponse<InterestResponse>> list(
    @Parameter(description = "검색 키워드 (이름·키워드 부분일치, 대소문자 무시)")
    @RequestParam(required = false) String keyword,
    @Parameter(description = "정렬 기준: name | subscriberCount (기본: name)")
    @RequestParam(required = false) String sortBy,
    @Parameter(description = "정렬 방향: asc | desc (기본: asc)")
    @RequestParam(required = false) String direction,
    @Parameter(description = "커서 값 (마지막 아이템 UUID, 빈 문자열 = 첫 페이지)")
    @RequestParam(required = false) String cursor,
    @Parameter(description = "페이지 크기 (기본: 20)")
    @RequestParam(defaultValue = "20") int size,
    @Parameter(description = "요청자 ID (선택 — 없으면 subscribedByMe 항상 false)")
    @RequestHeader(value = "Monew-Request-User-ID", required = false) UUID userId) {
  ...
}
```

엔드포인트별 필요 어노테이션 요약:

| 엔드포인트 | summary | 응답코드 |
|-----------|---------|---------|
| GET /api/interests | 관심사 목록 조회 | 200, 400 |
| POST /api/interests | 관심사 등록 | 201, 400, 409 |
| PATCH /api/interests/{id} | 키워드 수정 | 200, 400, 404 |
| DELETE /api/interests/{id} | 관심사 삭제 | 204, 404 |
| POST /api/interests/{id}/subscriptions | 관심사 구독 | 201, 404, 409 |
| DELETE /api/interests/{id}/subscriptions | 구독 취소 | 204, 404 |

---

## 3. 미구현 항목 구현 가이드

### [미구현 1] MongoDB UserActivity 구독 연동 (T7 심화)

**기술 요구사항 T7**: 사용자 활동 시 UserActivity MongoDB document 선제 갱신  
**영향 파일**: `domain/interest/service/InterestSubscriptionService.java`

#### 현재 코드

```java
@Transactional
public SubscriptionResponse subscribe(UUID interestId, UUID userId) {
    // ... 구독 저장 + 카운터 증가
    // ❌ MongoDB UserActivity 업데이트 없음
    return SubscriptionResponse.from(saved);
}

@Transactional
public void unsubscribe(UUID interestId, UUID userId) {
    // ... 구독 삭제 + 카운터 감소
    // ❌ MongoDB UserActivity 업데이트 없음
}
```

#### 구현 방법

**선행 조건**: `UserActivity` MongoDB 도메인 구현 필요 (`domain/activity/` 또는 `domain/user/activity/`)

`UserActivity` document 구조 (T7 요구사항):
```java
@Document(collection = "user_activities")
public class UserActivity {
    @Id
    private UUID userId;
    private List<SubscribedInterest> subscribedInterests; // 구독 관심사 목록
    private List<CommentActivity> recentComments;         // 최근 댓글 10개
    private List<LikeActivity> recentLikes;               // 최근 좋아요 10개
    private List<ArticleView> recentArticles;             // 최근 본 기사 10개
}
```

`InterestSubscriptionService` 수정:
```java
@Service
@RequiredArgsConstructor
public class InterestSubscriptionService {

    private final InterestRepository interestRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserActivityService userActivityService; // 추가

    @Transactional
    public SubscriptionResponse subscribe(UUID interestId, UUID userId) {
        Interest interest = interestRepository.findByIdAndDeletedAtIsNull(interestId)
            .orElseThrow(...);

        Subscription saved;
        try {
            saved = subscriptionRepository.saveAndFlush(new Subscription(interestId, userId));
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateSubscriptionException(...);
        }

        interestRepository.incrementSubscriberCount(interest.getId());

        // ✅ MongoDB UserActivity 구독 관심사 추가
        userActivityService.addSubscribedInterest(userId, interest);

        return SubscriptionResponse.from(saved);
    }

    @Transactional
    public void unsubscribe(UUID interestId, UUID userId) {
        Subscription sub = subscriptionRepository.findByInterestIdAndUserId(interestId, userId)
            .orElseThrow(...);

        subscriptionRepository.delete(sub);
        interestRepository.decrementSubscriberCount(interestId);

        // ✅ MongoDB UserActivity 구독 관심사 제거
        userActivityService.removeSubscribedInterest(userId, interestId);
    }
}
```

**주의사항**:
- PostgreSQL 트랜잭션 커밋 후 MongoDB 업데이트 실행 → 두 저장소 간 원자성 보장 불가
- 단순 구현: 동기 호출 (현재 학습 프로젝트 수준에서 권장)
- 고급 구현: `@TransactionalEventListener(phase = AFTER_COMMIT)`로 PostgreSQL 커밋 후 MongoDB 업데이트
- UserActivity document 없는 경우 `upsert` 처리 필수 (신규 사용자)

---

### [미구현 2] Notification 알림 트리거 연동

**기능 요구사항**: 구독 관심사에 신규 기사 등록 시 알림 생성  
**알림 메시지**: `"[관심사명]와 관련된 기사가 N건 등록되었습니다."`  
**트리거 포인트**: Article 수집 배치(`ArticleCollectionJob`)에서 기사 저장 후 호출

#### Interest 도메인에서 필요한 인터페이스

`SubscriptionRepository`에 구독자 ID 조회 메서드 추가 필요:

```java
// SubscriptionRepository.java에 추가
@Query("SELECT s.userId FROM Subscription s WHERE s.interestId = :interestId")
List<UUID> findUserIdsByInterestId(@Param("interestId") UUID interestId);
```

#### Article 배치에서 호출할 흐름

```
ArticleCollectionJob
  → 기사 저장 (articleRepository.save)
  → 해당 관심사 구독자 ID 조회 (subscriptionRepository.findUserIdsByInterestId)
  → 구독자별 Notification 생성 (notificationService.createNotification)
```

**NotificationService 인터페이스** (최건위 담당 MON-80과 연동):
```java
// 기사 등록 알림 생성 예시 (NotificationService)
public void notifyNewArticles(UUID interestId, String interestName, int articleCount, List<UUID> subscriberIds) {
    String message = String.format("[%s]와 관련된 기사가 %d건 등록되었습니다.", interestName, articleCount);
    for (UUID userId : subscriberIds) {
        createNotification(userId, message, ResourceType.INTEREST, interestId);
    }
}
```

---

### [미구현 3] CursorPageResponse 위치 이동 (전체 도메인 적용 시)

**현재 위치**: `domain/interest/dto/CursorPageResponse.java`  
**문제**: Interest 전용 패키지에 있어 다른 도메인에서 임포트 시 패키지 의존성 오염

**권장 이동 경로**: `global/dto/CursorPageResponse.java`

**이동 절차**:
1. `src/main/java/com/example/monew/global/dto/CursorPageResponse.java` 생성 (내용 동일)
2. 기존 `domain/interest/dto/CursorPageResponse.java` 삭제
3. 아래 파일 임포트 경로 변경:
   - `InterestController.java`
   - `InterestService.java`
   - `InterestControllerTest.java`
   - `InterestServiceTest.java`
   - `InterestApiIntegrationTest.java`
4. `./gradlew test --tests "com.example.monew.domain.interest.*"` 재실행 확인

**참고**: Article 도메인의 `CursorPageResponseArticleDto`는 `nextAfter(LocalDateTime)` 추가 필드가 있어 구조가 다름 → 현행 유지, `CursorPageResponse<T>`로 통합 불가.

---

### [미구현 4] MDC 로깅 인터셉터 (T3)

**기술 요구사항 T3**: 모든 요청에 `requestId`, `clientIp` MDC 세팅 + 응답 헤더 `MoNew-Request-ID` 포함  
**Interest 도메인 직접 관련 없음** — 공통 인터셉터로 구현

**구현 위치**: `global/interceptor/MdcLoggingInterceptor.java`

```java
@Component
public class MdcLoggingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        String requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId);
        MDC.put("clientIp", request.getRemoteAddr());
        MDC.put("requestMethod", request.getMethod());
        MDC.put("requestUrl", request.getRequestURI());
        response.setHeader("MoNew-Request-ID", requestId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler, Exception ex) {
        MDC.clear();
    }
}
```

**WebMvcConfigurer 등록**:
```java
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final MdcLoggingInterceptor mdcLoggingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(mdcLoggingInterceptor)
                .addPathPatterns("/api/**");
    }
}
```

**logback-spring.xml 패턴 예시**:
```xml
<pattern>[%X{requestId}] [%X{clientIp}] %d{HH:mm:ss} %-5level %logger{36} - %msg%n</pattern>
```

---

## 4. 작업 우선순위 요약

| 우선순위 | 항목 | 담당 | 난이도 | 선행 조건 |
|---------|------|------|--------|---------|
| **P2** | `updateKeywords` subscribedByMe 수정 | 이종호 | 낮음 | 없음 |
| **P2** | Swagger 어노테이션 추가 | 이종호 | 낮음 | 없음 |
| **P2** | 필드명 `service` → `interestSubscriptionService` | 이종호 | 낮음 | 없음 |
| **P2** | `CursorPageResponse` global 이동 | 이종호 | 중간 | 전체 테스트 재실행 |
| **P3** | MDC 로깅 인터셉터 | 공통 | 중간 | 없음 |
| **P3** | MongoDB UserActivity 구독 연동 | 이종호 | 높음 | UserActivity 도메인 구현 |
| **P3** | Notification 알림 트리거 | 최건위+이종호 | 높음 | MON-80 NotificationService |

---

## 5. 검증 커맨드

```bash
# Interest 도메인 전체 테스트
./gradlew test --tests "com.example.monew.domain.interest.*"

# 전체 빌드 + 테스트
./gradlew clean test

# 커버리지 확인
./gradlew jacocoTestReport
# 결과: build/reports/jacoco/test/html/index.html
```
