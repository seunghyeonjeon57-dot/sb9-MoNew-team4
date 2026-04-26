# Interest 도메인 코드 리뷰

> 작성일: 2026-04-24
> 기준 브랜치: `develop`
> 검증 방식: 7개 병렬 에이전트 교차 검증 (Entity·Repository·Service·Subscription·Controller/DTO·예외·컨벤션)
> 목적: 현재 구현 상태를 팀원이 이해할 수 있도록 레이어별로 정리

---

## 1. 구현 현황 요약

Swagger 스펙 기준 Interest 도메인 API 6개 전부 구현 완료. 현재 **89개 테스트 통과** 상태.

| 엔드포인트 | HTTP | 상태 |
|-----------|------|------|
| `/api/interests` | POST | ✅ 완료 |
| `/api/interests` | GET (커서 페이지네이션) | ✅ 완료 |
| `/api/interests/{id}` | PATCH (키워드 수정) | ✅ 완료 |
| `/api/interests/{id}` | DELETE | ✅ 완료 |
| `/api/interests/{id}/subscriptions` | POST | ✅ 완료 |
| `/api/interests/{id}/subscriptions` | DELETE | ✅ 완료 |

---

## 2. 패키지 구조

```
domain/interest/
├── controller/
│   ├── InterestController.java           # CRUD 4개 엔드포인트
│   └── InterestSubscriptionController.java  # 구독/취소 2개 엔드포인트
├── dto/
│   ├── InterestCreateRequest.java
│   ├── InterestUpdateRequest.java
│   ├── InterestResponse.java
│   ├── SubscriptionResponse.java
│   └── CursorPageResponse.java           # 제네릭 커서 페이지 응답
├── entity/
│   ├── Interest.java                     # 관심사 (BaseEntity 상속)
│   ├── InterestKeyword.java              # 키워드 (일대다 자식)
│   └── Subscription.java                # 구독 (BaseEntity 상속)
├── exception/
│   ├── DuplicateSubscriptionException.java
│   ├── InterestNameImmutableException.java
│   ├── InterestNotFoundException.java
│   ├── InvalidSortParameterException.java
│   ├── SimilarInterestNameException.java
│   ├── SubscriberNotFoundException.java
│   └── SubscriptionNotFoundException.java
├── repository/
│   ├── InterestRepository.java           # JPA + 커스텀 인터페이스 결합
│   ├── InterestRepositoryCustom.java     # CursorPage 반환 정의
│   ├── InterestRepositoryImpl.java       # QueryDSL keyset 구현
│   └── SubscriptionRepository.java
└── service/
    ├── InterestService.java              # 관심사 CRUD + 목록 조회
    └── InterestSubscriptionService.java  # 구독/취소
```

---

## 3. Entity 레이어

### 3.1 Interest

```java
@Entity @Table(name = "interests")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Interest extends BaseEntity {

    @Id @Column(columnDefinition = "uuid")
    private UUID id = UUID.randomUUID();

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @OneToMany(mappedBy = "interest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InterestKeyword> keywords = new ArrayList<>();

    @Column(nullable = false)
    private long subscriberCount = 0L;

    @Builder
    public Interest(String name, List<String> keywords) { ... }

    public void replaceKeywords(List<String> newKeywords) { ... }
}
```

**설계 포인트**

- `BaseEntity` 상속으로 `createdAt`, `updatedAt`, `deletedAt` 자동 관리
- `id = UUID.randomUUID()` 필드 선언 시 초기화 → JPA persist 전에 이미 UUID 보유
- `@NoArgsConstructor(access = PROTECTED)` → 외부에서 기본 생성자 직접 호출 불가, JPA 프록시만 허용
- `keywords` 컬렉션은 `CascadeType.ALL` + `orphanRemoval = true` → `replaceKeywords()` 호출 시 기존 키워드 자동 삭제 후 신규 등록
- `subscriberCount`는 엔티티 setter 없이 Repository 레이어 JPQL UPDATE 쿼리로만 변경 (원자성 보장)

**주의할 점**

Builder 생성자 내부에서 `IllegalArgumentException`을 직접 던집니다.

```java
// Interest.java:41, 44
if (!StringUtils.hasText(name)) {
    throw new IllegalArgumentException("관심사 이름은 비어 있을 수 없습니다.");
}
```

컨트롤러 `@Valid`가 먼저 차단하므로 런타임 동작에는 문제 없지만, 팀의 `MonewException` 계약과 불일치합니다. 엔티티를 직접 생성하는 배치나 테스트 코드에서 이 예외가 나오면 `GlobalException`의 `IllegalArgumentException` 핸들러(→ 400)로 처리됩니다.

---

### 3.2 InterestKeyword

```java
@Entity @Table(name = "interest_keywords")
public class InterestKeyword {
    @Column(name = "keyword_value", nullable = false)
    private String value;
}
```

> ⚠️ **스키마 불일치 주의**
> `@Column(name = "keyword_value")`로 선언됐지만 `src/main/resources/schema.sql`의 컬럼명은 `value`입니다.
> `test` 프로파일은 `ddl-auto: create`여서 Hibernate가 엔티티 기준으로 테이블을 새로 만들기 때문에 테스트는 통과합니다.
> `dev` 환경에서 schema.sql을 직접 적용하면 컬럼명 불일치로 쿼리 오류가 발생할 수 있습니다.
> 수정: `@Column(name = "keyword_value")` → `@Column(name = "value")`

---

### 3.3 Subscription

```java
@Entity @Table(name = "subscriptions")
public class Subscription extends BaseEntity {
    @Id UUID id = UUID.randomUUID();
    @Column(name = "interest_id") UUID interestId;
    @Column(name = "user_id")    UUID userId;
}
```

- schema.sql에 `CONSTRAINT uk_user_interest UNIQUE (user_id, interest_id)` 선언 → DB 수준 중복 구독 방지
- 연관관계 없이 FK를 UUID로만 보관 → Interest/User 엔티티 로드 없이 경량 처리 가능

---

## 4. Repository 레이어

### 4.1 InterestRepository

```java
public interface InterestRepository extends JpaRepository<Interest, UUID>,
                                            InterestRepositoryCustom {

    Optional<Interest> findByNameAndDeletedAtIsNull(String name);
    List<Interest> findAllByDeletedAtIsNull();
    Optional<Interest> findByIdAndDeletedAtIsNull(UUID id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Interest i SET i.subscriberCount = i.subscriberCount + 1 WHERE i.id = :id")
    void incrementSubscriberCount(@Param("id") UUID id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Interest i SET i.subscriberCount = i.subscriberCount - 1 WHERE i.id = :id")
    void decrementSubscriberCount(@Param("id") UUID id);
}
```

**설계 포인트**

- `findByIdAndDeletedAtIsNull`: soft delete 필터를 쿼리 메서드명에 명시. `findById` 단독 사용 금지.
- `incrementSubscriberCount`에 `clearAutomatically = true` 설정 → UPDATE 후 1차 캐시 자동 evict. 이를 설정하지 않으면 JPQL UPDATE 후 같은 트랜잭션에서 `findById`를 호출해도 캐시의 old 값을 반환하는 버그가 발생합니다. (MON-125 이슈에서 발견, TDD로 수정)

---

### 4.2 InterestRepositoryImpl (QueryDSL Keyset 페이지네이션)

이 구현이 Interest 도메인의 가장 핵심 로직입니다.

```java
@Override
public CursorPage findByCursor(
    String keywordArg, String orderBy, String direction,
    UUID cursorId, LocalDateTime after, int limit) {

    // 1. 기본 필터: soft delete 제외 + 키워드 검색
    BooleanBuilder base = new BooleanBuilder();
    base.and(interest.deletedAt.isNull());

    // 2. 커서 조건 추가 (첫 페이지 = 커서 없음)
    BooleanExpression cursorWhere = buildCursorWhere(orderBy, direction, cursorId, after);
    if (cursorWhere != null) { pageWhere.and(cursorWhere); }

    // 3. limit + 1개 조회 → hasNext 판단
    List<Interest> rows = queryFactory.selectFrom(interest)
        .where(pageWhere)
        .orderBy(buildOrder(orderBy, direction))
        .limit(limit + 1L)
        .fetch();

    // 4. 전체 카운트 (필터 조건만, 커서 조건 제외)
    long total = queryFactory.select(interest.count()).from(interest).where(base).fetchOne();

    boolean hasNext = rows.size() > limit;
    List<Interest> content = hasNext ? rows.subList(0, limit) : rows;
    return new CursorPage(content, total, hasNext);
}
```

**Keyset 정렬 기준 3단계 복합키**

단순 `createdAt` 커서는 동시에 등록된 관심사가 있으면 같은 페이지에 중복 노출될 수 있습니다. 이를 방지하기 위해 3단계 복합키를 사용합니다.

```
정렬: [primary(name or subscriberCount)] → [createdAt ASC] → [id ASC]
```

```java
// fullKeyset() — subscriberCount DESC 기준 예시
if ("subscriberCount".equals(orderBy)) {
    long av = anchor.getSubscriberCount();
    return interest.subscriberCount.lt(av)                          // primary
        .or(interest.subscriberCount.eq(av).and(interest.createdAt.gt(ac)))  // tie-break 1
        .or(interest.subscriberCount.eq(av).and(interest.createdAt.eq(ac))
            .and(interest.id.gt(aid)));                             // tie-break 2
}
```

**커서 fallback 처리**

`cursorId`가 있지만 해당 관심사가 삭제된 경우, `buildCursorWhere`가 anchor를 찾지 못합니다. 이때 `after(LocalDateTime)` 파라미터가 있으면 timestamp 기반 keyset으로 fallback합니다.

```java
if (cursorId != null) {
    Interest anchor = queryFactory.selectFrom(interest)
        .where(interest.id.eq(cursorId).and(interest.deletedAt.isNull()))
        .fetchOne();
    if (anchor != null)  return fullKeyset(orderBy, direction, anchor);
    if (after != null)   return timestampKeyset(after, cursorId);  // fallback
    return null;
}
```

**키워드 검색 구현**

이름 포함 검색 OR 키워드 서브쿼리 존재 검색을 결합합니다.

```java
private BooleanExpression keywordCondition(String kw) {
    return interest.name.containsIgnoreCase(kw)
        .or(JPAExpressions.selectOne()
            .from(keyword)
            .where(keyword.interest.id.eq(interest.id)
                .and(keyword.value.containsIgnoreCase(kw)))
            .exists());
}
```

---

### 4.3 SubscriptionRepository

```java
@Query("SELECT s.interestId FROM Subscription s WHERE s.userId = :userId AND s.interestId IN :interestIds")
Set<UUID> findInterestIdsByUserIdAndInterestIdIn(@Param("userId") UUID userId,
                                                  @Param("interestIds") Collection<UUID> interestIds);
```

목록 조회에서 `subscribedByMe` 필드를 채울 때 이 쿼리를 사용합니다. N+1 대신 단일 IN 쿼리로 현재 페이지의 모든 관심사에 대한 구독 여부를 한 번에 가져옵니다.

```java
// InterestService.subscribedIdsFor()
private Set<UUID> subscribedIdsFor(UUID userId, List<Interest> filtered) {
    if (userId == null || filtered.isEmpty()) return Set.of();
    Collection<UUID> ids = filtered.stream().map(Interest::getId).toList();
    return subscriptionRepository.findInterestIdsByUserIdAndInterestIdIn(userId, ids);
}
```

---

## 5. Service 레이어

### 5.1 InterestService

**관심사 생성 — 유사도 차단 로직**

생성 요청이 들어오면 두 단계 검사를 순서대로 수행합니다.

```java
// 1단계: 정확히 같은 이름 존재 시 즉시 거부
interestRepository.findByNameAndDeletedAtIsNull(request.name())
    .ifPresent(existing -> {
        throw new SimilarInterestNameException(
            Map.of("existing", existing.getName(), "similarity", 1.0));
    });

// 2단계: 전체 활성 관심사와 Levenshtein 유사도 비교
List<Interest> actives = interestRepository.findAllByDeletedAtIsNull();
for (Interest existing : actives) {
    double similarity = SimilarityUtils.similarity(existing.getName(), request.name());
    if (similarity >= SIMILARITY_THRESHOLD) { // 0.8
        throw new SimilarInterestNameException(...);
    }
}
```

`SimilarityUtils.similarity()`는 Levenshtein 거리를 정규화한 값 (0.0~1.0)을 반환합니다. 80% 이상 유사하면 `SimilarInterestNameException` (409 CONFLICT) 발생.

**관심사 목록 조회 흐름**

```
1. validateSortParams() — orderBy/direction 허용값 체크 (위반 시 400)
2. parseCursorUuid()     — cursor 문자열 → UUID 변환 (잘못된 형식 시 400)
3. findByCursor()        — QueryDSL keyset 조회 (limit+1)
4. subscribedIdsFor()    — IN 쿼리로 구독 여부 벌크 조회
5. InterestResponse.from() — 엔티티 → DTO 변환 (subscribedByMe 주입)
6. CursorPageResponse 조립 — nextCursor, nextAfter, hasNext 계산
```

**관심사 삭제 흐름**

```java
@Transactional
public void delete(UUID interestId) {
    Interest interest = interestRepository.findByIdAndDeletedAtIsNull(interestId)
        .orElseThrow(() -> new InterestNotFoundException(...));
    subscriptionRepository.deleteAllByInterestId(interestId); // 구독 레코드 먼저 삭제
    interestRepository.delete(interest);                       // 관심사 삭제
}
```

관심사 삭제 전 해당 관심사의 구독 레코드를 먼저 삭제하여 FK 제약 위반을 방지합니다. 현재는 물리 삭제(hard delete)입니다. Swagger 스펙의 operationId도 `hardDelete`로 명시되어 있습니다.

---

### 5.2 InterestSubscriptionService

**구독 등록 — DB 제약 기반 중복 방지**

```java
@Transactional
public SubscriptionResponse subscribe(UUID interestId, UUID userId) {
    Interest interest = interestRepository.findByIdAndDeletedAtIsNull(interestId)
        .orElseThrow(() -> new InterestNotFoundException(...));

    Subscription saved;
    try {
        saved = subscriptionRepository.saveAndFlush(new Subscription(interestId, userId));
    } catch (DataIntegrityViolationException e) {
        translateIntegrityViolation(interestId, userId, e); // 제약명으로 분기
        throw e;
    }

    interestRepository.incrementSubscriberCount(interest.getId());
    return SubscriptionResponse.of(saved, interest);
}
```

```java
private void translateIntegrityViolation(UUID interestId, UUID userId, DataIntegrityViolationException e) {
    String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
    if (msg.contains("uk_user_interest")) {
        throw new DuplicateSubscriptionException(Map.of("interestId", interestId, "userId", userId));
    }
    if (msg.contains("fk_sub_user")) {
        throw new SubscriberNotFoundException(Map.of("userId", userId));
    }
}
```

`saveAndFlush` 직후 `DataIntegrityViolationException`이 발생하면 DB 제약 이름으로 분기합니다.
- `uk_user_interest` 위반 → `DuplicateSubscriptionException` (409)
- `fk_sub_user` 위반 → `SubscriberNotFoundException` (404)

---

## 6. Controller 레이어

### 6.1 InterestController

```java
@RestController
@RequestMapping("/api/interests")
@RequiredArgsConstructor
@Tag(name = "interests")
public class InterestController {

    @GetMapping
    @Operation(summary = "관심사 목록 조회")
    public ResponseEntity<CursorPageResponse<InterestResponse>> list(
        @RequestParam(required = false) String keyword,
        @RequestParam String orderBy,
        @RequestParam String direction,
        @RequestParam(required = false) String cursor,
        @RequestParam(required = false) LocalDateTime after,
        @RequestParam int limit,
        @RequestHeader(value = "Monew-Request-User-ID", required = false) UUID userId) { ... }

    @PostMapping
    public ResponseEntity<InterestResponse> create(
        @Valid @RequestBody InterestCreateRequest request) { ... } // 201 반환

    @PatchMapping("/{id}")
    public ResponseEntity<InterestResponse> updateKeywords(
        @RequestHeader("Monew-Request-User-ID") UUID userId,
        @PathVariable UUID id,
        @Valid @RequestBody InterestUpdateRequest request) { ... }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @RequestHeader("Monew-Request-User-ID") UUID userId,
        @PathVariable UUID id) { ... } // 204 반환
}
```

`@Valid`로 Request DTO 검증 후 서비스에 위임하는 구조. 모든 엔드포인트에 `@Operation`, `@ApiResponses` 적용 완료.

`@PathVariable UUID id` — 변수명이 `id`이지만 URL 경로 변수명은 `{id}` → Spring이 이름으로 매핑하므로 런타임 동작은 정상입니다. 다만 Swagger 스펙에서 path variable 이름은 `interestId`이므로 문서 일치성을 위해 `@PathVariable("id") UUID interestId` 또는 변수명을 `interestId`로 통일하는 것이 좋습니다.

### 6.2 InterestSubscriptionController

구독/취소 전용 컨트롤러. URL이 `/api/interests/{interestId}/subscriptions`이므로 `InterestController`의 `@RequestMapping("/api/interests")`와 충돌을 피하기 위해 분리됐습니다. 팀 컨벤션 "도메인 하나에 컨트롤러 하나"와 형식상 불일치하지만, 중첩 리소스 URL 구조상 실용적인 선택입니다.

---

## 7. DTO 레이어

### 7.1 요청 DTO

**InterestCreateRequest**
```java
public record InterestCreateRequest(
    @NotBlank @Size(min = 1, max = 50) String name,
    @NotEmpty @Size(min = 1, max = 10) List<@NotBlank String> keywords
) {}
```

**InterestUpdateRequest**
```java
public record InterestUpdateRequest(
    @Null String name,         // name 수정 불가 — null 아니면 400
    @NotEmpty @Size(min = 1, max = 10) List<@NotBlank String> keywords
) {}
```

`@Null`을 name 필드에 사용하는 패턴이 인상적입니다. 클라이언트가 name을 보내면 Bean Validation이 즉시 거부합니다. 서비스에서 별도로 체크하는 것보다 선언적이고 명확합니다.

### 7.2 응답 DTO

MapStruct 미사용. 정적 팩토리 패턴 `from()` / `of()`로 엔티티 → DTO 변환.

```java
// InterestResponse.java
public static InterestResponse from(Interest interest, boolean subscribedByMe) {
    return new InterestResponse(
        interest.getId(),
        interest.getName(),
        interest.getKeywords().stream().map(InterestKeyword::getValue).toList(),
        interest.getSubscriberCount(),
        subscribedByMe
    );
}
```

`CursorPageResponse<T>`는 현재 `domain/interest/dto/`에 있습니다. 다른 도메인이 커서 페이지네이션을 도입할 때 `global/dto/`로 이동을 고려해야 합니다.

---

## 8. 예외 처리 계층

모든 비즈니스 실패는 `MonewException → ErrorCode → GlobalException` 단일 경로를 따릅니다.

**Interest 도메인 ErrorCode 목록**

- `INTEREST_NOT_FOUND` → 404
- `SIMILAR_INTEREST_NAME` → 409
- `INTEREST_NAME_IMMUTABLE` → 400
- `INVALID_SORT_PARAMETER` → 400
- `DUPLICATE_SUBSCRIPTION` → 409
- `SUBSCRIPTION_NOT_FOUND` → 404

모든 예외 클래스는 `MonewException`을 상속하며, 생성 시 `details Map`에 진단 정보를 담습니다.

```java
throw new SimilarInterestNameException(
    Map.of("existing", existing.getName(), "similarity", similarity));
```

응답 바디 예시:
```json
{
  "code": "SIMILAR_INTEREST_NAME",
  "message": "이미 유사한 관심사 이름이 존재합니다.",
  "details": { "existing": "인공지능", "similarity": 0.87 },
  "timestamp": "2026-04-24T10:00:00"
}
```

---

## 9. 테스트 구조

총 89개 테스트. 4개 레이어 모두 커버.

| 레이어 | 파일 | 테스트 수 | 어노테이션 |
|--------|------|---------|----------|
| Entity Unit | InterestTest | 8 | 순수 JUnit |
| Entity Unit | SubscriptionTest | 5 | 순수 JUnit |
| Exception Unit | InterestExceptionsTest | 6 | 순수 JUnit |
| JPA Slice | InterestRepositoryTest | 7 | @DataJpaTest |
| JPA Slice | InterestRepositoryImplTest | 14 | @DataJpaTest |
| JPA Slice | SubscriptionRepositoryTest | 6 | @DataJpaTest |
| Service Unit | InterestServiceTest | 15 | @ExtendWith(MockitoExtension) |
| Service Unit | InterestSubscriptionServiceTest | 7 | @ExtendWith(MockitoExtension) |
| Web Slice | InterestControllerTest | 18 | @WebMvcTest |
| Web Slice | InterestSubscriptionControllerTest | 8 | @WebMvcTest |
| Integration | InterestApiIntegrationTest | 10 | @SpringBootTest |

**WebMvcTest 설정 체크포인트**

모든 WebMvcTest에 `@Import(GlobalException.class)` 적용 완료. 이를 빠뜨리면 예외 응답이 핸들러가 없어서 500으로 떨어지므로 예외 계약 검증이 의미 없어집니다.

**JPA Slice flush/clear 패턴**

리포지토리 테스트에서 `flushAutomatically=true` 옵션이 있는 JPQL UPDATE를 검증할 때, 이 패턴이 일관되게 적용됩니다.

```java
interestRepository.incrementSubscriberCount(saved.getId()); // JPQL UPDATE
em.flush();
em.clear(); // 1차 캐시 제거 — 이후 findById는 DB 재조회
Interest refreshed = interestRepository.findByIdAndDeletedAtIsNull(saved.getId()).orElseThrow();
assertThat(refreshed.getSubscriberCount()).isEqualTo(1L);
```

`clear()` 없이 `findById`를 바로 호출하면 1차 캐시의 오래된 값이 반환됩니다. (MON-125에서 이 버그를 TDD로 발견)

**TDD 커밋 패턴**

모든 기능이 `[Red-Unit] → [Green-Unit] → [Test-Integration]` 패턴으로 커밋됨. 예시:
```
[MON-125] [Red-Unit]   Test: incrementSubscriberCount 후 findById 1차 캐시 stale 재현
[MON-125] [Green-Unit] Fix: clearAutomatically = true 옵션 추가
```

---

## 10. 발견된 이슈

### P1 — InterestKeyword 컬럼명 불일치

- 파일
  - `InterestKeyword.java` 25행
  - `schema.sql` 32행

- 현재 코드
  - `@Column(name = "keyword_value")` — 엔티티 선언
  - `value VARCHAR(50)` — schema.sql 실제 컬럼명

- 영향
  - `test` 프로파일은 `ddl-auto: create`이므로 테스트는 통과
  - `dev`에서 schema.sql로 DB를 초기화하면 컬럼명 불일치로 INSERT/SELECT 오류 가능

- 수정
  - `@Column(name = "keyword_value")` → `@Column(name = "value")`

---

### P2 — MON-146: Swagger 스펙에 409 응답 누락

- 파일
  - `InterestSubscriptionController.java` (구독 POST 엔드포인트)
  - `docs/monew-swagger.json`

- 현황
  - 구현: `DuplicateSubscriptionException` → 409 DUPLICATE_SUBSCRIPTION 정상 반환
  - Swagger 스펙: 200 / 404 / 500만 명시 (409 누락)
  - 테스트 주석으로 알려진 이슈 처리 중 (InterestSubscriptionServiceTest.java 121행)

- 수정
  - swagger.json 해당 endpoint responses에 409 추가

---

### P3 — PathVariable 이름과 Swagger 스펙 불일치

- 파일
  - `InterestController.java` 93행 (`@PathVariable UUID id`)
  - `InterestController.java` 109행 (`@PathVariable UUID id`)

- 현황
  - 컨트롤러 변수명은 `id`, Swagger 스펙의 path variable 이름은 `interestId`
  - Spring이 URL 패턴 `/{id}`으로 바인딩하므로 런타임 동작은 정상

- 수정 옵션
  - `@PathVariable UUID id` → `@PathVariable UUID interestId`로 변수명 통일

---

### P3 — SubscriptionRepository 인터페이스에 @Transactional 선언

- 파일
  - `SubscriptionRepository.java` 32~38행

- 현황
  - `deleteAllByInterestId()`와 `deleteAllByUserId()`에 `@Transactional` 적용
  - 이 메서드들은 이미 `@Transactional` 서비스에서 호출되므로 동작에 문제 없음
  - 팀 컨벤션: 트랜잭션은 서비스 레이어에서 관리, 리포지토리 인터페이스에는 배치하지 않음

- 수정
  - 리포지토리의 `@Transactional` 제거, 서비스에서 트랜잭션 보장

---

### P3 — buildCursorWhere() 조건 중첩 깊이

- 파일
  - `InterestRepositoryImpl.java` 75~93행

- 현황
  - if 블록 최대 깊이 3단계 (컨벤션 권고: 2단계 이하)
  - 커서 조건 분기 특성상 자연스러운 구조지만, early return 리팩터링으로 줄일 수 있음

- 리팩터링 방향 예시
  ```java
  private BooleanExpression buildCursorWhere(...) {
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

---

## 11. 잘 구현된 점 하이라이트

- QueryDSL keyset 3단계 복합키 페이지네이션: 동률 레코드도 안정적으로 처리
- `clearAutomatically = true` 옵션으로 JPQL UPDATE 후 1차 캐시 오염 방지 (MON-125)
- `findByIdAndDeletedAtIsNull` 패턴 일관 적용: `findById` 단독 사용 없음
- N+1 방지 IN 쿼리: `subscribedByMe` 처리 시 단일 쿼리로 현재 페이지 전체 처리
- `DataIntegrityViolationException` 제약명 분기로 DB 레벨 중복 방지와 비즈니스 예외 연결
- `@Transactional(readOnly = true)` 읽기 메서드 일관 적용
- WebMvcTest 전체에 `@Import(GlobalException.class)` 적용
- 89개 테스트 TDD Red-Green 패턴 및 엣지 케이스 커버 (커서 fallback, 동률, 공백 키워드 등)
