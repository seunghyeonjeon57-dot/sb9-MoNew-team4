# Interest 도메인 + 공통 예외 인프라 핸드오프

> **대상 독자**: 본 도메인을 이어받아 TDD로 수정/추가 작업을 계속할 팀원
> **범위**: `feat/exception-infra` (PR #8) + `feat/interest` (PR #9) 까지 반영된 코드
> **작성 시점**: 2026-04-15

---

## 0. 한눈에 보기

| 영역 | PR | base | 상태 | 주요 모듈 |
|---|---|---|---|---|
| 공통 예외 인프라 | #8 (`feat/exception-infra`) | `develop` | 머지 대기 | `global/exception/*`, `application.yaml` 로깅 패턴 |
| Interest 도메인 | #9 (`feat/interest`) | `feat/exception-infra` → 인프라 머지 후 `develop` 재베이스 예정 | 진행 중 (I1~I6 + E2E 완료, 일부 보강 남음) | `domain/interest/**`, `global/util/SimilarityUtils` |

**Jira 티켓 체계**: MON-XX. PR 제목은 **단일 키** (`[MON-27] ...`)만 자동 연동된다. 여러 티켓이 걸릴 때는 제목은 대표 키 하나만 적고, PR 본문 첫 줄에 `**관련 티켓**: MON-27 MON-28 ...` 형태로 공백 구분하여 나열한다. `[MON-27~58]` 같은 범위 표기는 GitHub for Jira 앱 정규식(`[A-Z]+-\d+`)과 맞지 않아 연동이 끊긴다.

**모뉴프로젝트 **
- 기능 요구사항: `모뉴 노션 문서`
- 기술 요구사항: `모뉴 노션 문서`
- 스웨거/테스트 플랜: `http://sprint-project-1196140422.ap-northeast-2.elb.amazonaws.com/sb/monew/api/swagger-ui/index.html`
---

## 1. 아키텍처 개요

### 1-1. 계층 구조

```
┌──────────────────────────────────────────────────┐
│ Controller  (요청 수신 / @RequestHeader / @Valid) │
├──────────────────────────────────────────────────┤
│ Service     (트랜잭션 경계 / 비즈 규칙)           │
├──────────────────────────────────────────────────┤
│ Repository  (JpaRepository, 소프트 삭제 필터)     │
├──────────────────────────────────────────────────┤
│ Entity      (불변식 / 도메인 메서드)              │
└──────────────────────────────────────────────────┘
       ▲
       │ 전 계층 공용
┌──────┴─────────────────────────────────┐
│ global/exception: ErrorCode, MonewException, ErrorResponse, GlobalException │
│ global/util:      SimilarityUtils                                           │
└────────────────────────────────────────┘
```

### 1-2. Stacked PR 흐름

1. `feat/exception-infra` (PR #8) 가 `develop`에 머지되면
2. `feat/interest` (PR #9) 를 `upstream/develop`로 rebase → base 를 `develop`으로 변경
3. 후속 도메인(User/Article/Comment/Notification)은 모두 인프라 머지 이후 `develop` base 로 분기

### 1-3. 테스트 전략 매트릭스

| 계층 | 테스트 어노테이션 | 대표 파일 |
|---|---|---|
| 엔티티/POJO | (순수 JUnit5) | `entity/InterestTest.java`, `exception/InterestExceptionsTest.java` |
| 서비스 | `@ExtendWith(MockitoExtension.class) + @InjectMocks` | `service/InterestServiceTest.java` |
| 레포지토리 | `@DataJpaTest` (+ H2) | `repository/InterestRepositoryTest.java` |
| 컨트롤러 슬라이스 | `@WebMvcTest(Controller.class) + @Import(GlobalException.class) + @MockitoBean` | `controller/InterestControllerTest.java` |
| E2E | `@SpringBootTest + @AutoConfigureMockMvc + @AutoConfigureTestDatabase(Replace.ANY)` | `InterestApiIntegrationTest.java` |

---

## 2. 공통 예외 인프라

### 2-1. 파일 인벤토리

| 파일 (절대경로) | 라인 | 역할 |
|---|---|---|
| `src/main/java/com/example/monew/global/exception/ErrorCode.java` | 1-36 | 에러 코드 enum (공통 7 + Interest 6 + User 1 = **14개**) |
| `src/main/java/com/example/monew/global/exception/MonewException.java` | 1-25 | 커스텀 예외 abstract 베이스 (`timestamp` / `errorCode` / `details`) |
| `src/main/java/com/example/monew/global/exception/ErrorResponse.java` | 1-70 | 응답 클래스 + 정적 팩토리 3종 + MDC `request_id` 주입 |
| `src/main/java/com/example/monew/global/exception/GlobalException.java` | 1-80 | `@RestControllerAdvice` 핸들러 6종 |
| `src/main/resources/application.yaml` | 25-27 | 로깅 패턴 (`%X{request_id}`, `%X{client_ip}`) |

### 2-2. ErrorCode 전수 (14개)

| 이름 | HttpStatus | 기본 message | 분류 |
|---|---|---|---|
| `INVALID_REQUEST` | 400 | 요청이 올바르지 않습니다. | 공통 |
| `MALFORMED_REQUEST_BODY` | 400 | 요청 본문 형식이 올바르지 않습니다. | 공통 |
| `CONSTRAINT_VIOLATION` | 400 | 요청 파라미터 제약 조건을 위반했습니다. | 공통 |
| `MISSING_REQUEST_HEADER` | 400 | 필수 요청 헤더가 누락되었습니다. | 공통 |
| `METHOD_NOT_ALLOWED_REQUEST` | 405 | 허용되지 않은 HTTP 메서드입니다. | 공통 |
| `UNSUPPORTED_MEDIA_TYPE_REQUEST` | 415 | 지원하지 않는 미디어 타입입니다. | 공통 |
| `INTERNAL_ERROR` | 500 | 서버 내부 오류가 발생했습니다. | 공통 |
| `INTEREST_NOT_FOUND` | 404 | 해당 관심사를 찾을 수 없습니다. | Interest |
| `SIMILAR_INTEREST_NAME` | 409 | 이미 80% 이상 유사한 관심사 이름이 존재합니다. | Interest |
| `INTEREST_NAME_IMMUTABLE` | 400 | 관심사 이름은 수정할 수 없습니다. | Interest (선언만, 사용처 미구현) |
| `INVALID_SORT_PARAMETER` | 400 | 허용되지 않은 정렬 파라미터입니다. | Interest |
| `DUPLICATE_SUBSCRIPTION` | 409 | 이미 해당 관심사를 구독 중입니다. | Interest |
| `SUBSCRIPTION_NOT_FOUND` | 404 | 구독 정보를 찾을 수 없습니다. | Interest |
| `USER_NOT_FOUND` | 404 | 해당 유저를 찾을 수 없습니다. | User (기존 보존) |

### 2-3. MonewException 시그니처

```java
public abstract class MonewException extends RuntimeException {
  private final Instant timestamp;
  private final ErrorCode errorCode;
  private final Map<String, Object> details;

  protected MonewException(ErrorCode errorCode, Map<String, Object> details) { ... }
  protected MonewException(ErrorCode errorCode) { this(errorCode, new HashMap<>()); }
}
```

- `abstract` → 반드시 서브클래스로 확장 (`InterestNotFoundException extends MonewException` 등)
- `super(errorCode.getMessage())` 로 부모 `RuntimeException.message` 채움
- `timestamp` 는 인스턴스 생성 시각

### 2-4. ErrorResponse 구조

7필드: `timestamp`, `code`, `message`, `details`, `exceptionType`, `status`, `traceId`

정적 팩토리 3종:
- `ErrorResponse.of(ErrorCode)` — details 없는 기본 응답
- `ErrorResponse.of(ErrorCode, Map)` — details 포함
- `ErrorResponse.of(MonewException)` — 예외에서 code/status/details/exceptionType 자동 채움

`traceId` 는 `MDC.get("request_id")` 로 주입되므로, MDC에 `request_id` 가 세팅되어 있어야 응답과 로그가 연결된다.

### 2-5. GlobalException 핸들러 매핑

| @ExceptionHandler 대상 | 매핑 ErrorCode | HttpStatus | 위치 |
|---|---|---|---|
| `MonewException` | `exception.getErrorCode()` | errorCode.status | `GlobalException.java:27-34` |
| `MethodArgumentNotValidException` (`@Valid` 실패) | `INVALID_REQUEST` | 400, details에 fieldErrors | `GlobalException.java:36-50` |
| `MissingRequestHeaderException` | `MISSING_REQUEST_HEADER` | 400, details.header | `GlobalException.java:52-59` |
| `MethodArgumentTypeMismatchException` | `INVALID_REQUEST` | 400, details.parameter/value | `GlobalException.java:61-70` |
| `IllegalArgumentException` | `INVALID_REQUEST` | 400, details.reason | `GlobalException.java:72-79` |
| `Exception` (fallback) | `INTERNAL_ERROR` | 500, details.exceptionType | `GlobalException.java:18-25` |

### 2-6. 테스트 파일 + @DisplayName

**단위**
- `ErrorCodeTest.java` — 파라미터화 매트릭스 2건 (`{name} → {status}`, `{name} 메시지는 비어있지 않다`)
- `MonewExceptionTest.java` — 4건 (기본 생성자 / details 생성자 / ErrorCode null → NPE / abstract 강제)
- `ErrorResponseTest.java` — 3건 (`of(ErrorCode)`, `of(ErrorCode, details)`, `of(MonewException)`)
- `GlobalExceptionTest.java` — 2건 (`handleMonewException`, `handleException` fallback)

**통합**
- `GlobalExceptionWebMvcTest.java` — 7건 (MonewException / 런타임 / IAE / 타입미스매치 / fallback / 헤더누락 / @Valid 실패)

### 2-7. 새 ErrorCode/핸들러를 추가할 때

1. **ErrorCode 추가**: `ErrorCodeTest` 매트릭스 스트림에 한 줄 추가 → 실행 → Red 확인 → enum에 정의 → Green
2. **새 커스텀 예외**: `extends MonewException` 클래스를 `domain/<도메인>/exception/`에 추가 + 단위 테스트 (code/details 검증)
3. **새 핸들러**: `GlobalExceptionWebMvcTest`에 슬라이스 케이스 → Red → `GlobalException`에 `@ExceptionHandler` 메서드 → Green

---

## 3. Interest 도메인

### 3-1. 도메인 모델

```
Interest ─── 1:N (cascade=ALL, orphanRemoval) ──▶ InterestKeyword
  │              (mappedBy = "interest")           └─ @Column("keyword_value") ← H2 예약어 회피
  │                                                     (실제 필드명은 value)
  │
  └── 논리적 1:N (FK 없음, interestId 칼럼으로만 연결) ──▶ Subscription
                                                              └─ interestId, userId (둘 다 NotNull)
```

엔티티 ID는 모두 `private UUID id = UUID.randomUUID();` 필드 초기화 패턴 (JPA save 전에도 ID 확정).

### 3-2. 엔티티 핵심 메서드

**`Interest`** (`domain/interest/entity/Interest.java`)
| 메서드 | 책임 |
|---|---|
| `Interest(String name, List<String> keywords)` | 생성자: `name` 비어있으면 / `keywords` 비었으면 `IllegalArgumentException` |
| `replaceKeywords(List<String>)` | 기존 keywords 전체 clear 후 재생성 (orphanRemoval로 delete) |
| `markDeleted()` | soft delete flag 세팅 |
| `incrementSubscriberCount()` / `decrementSubscriberCount()` | 카운터 증감 (0 미만으로 내려가지 않음) |

**`InterestKeyword`** (`domain/interest/entity/InterestKeyword.java`)
- 필드 `value` 는 DB 컬럼명 `keyword_value` (라인 25). `value`는 H2 예약어이므로 반드시 이 매핑 유지.
- 생성자 가시성 package-private (`Interest` 만 생성 가능).

**`Subscription`** (`domain/interest/entity/Subscription.java`)
- `interestId`, `userId` null 체크 (Objects.requireNonNull).
- FK 없이 ID 쌍만 보관 → soft delete 된 Interest를 조회 때 필터링하는 전제.

### 3-3. Repository

**`InterestRepository`**
- `findAllByIsDeletedFalse()` — 활성 목록 (getInterests / 유사도 검사에서 사용)
- `findByIdAndIsDeletedFalse(UUID)` — 단건 조회 (delete/update/subscribe 경로)
- `findByNameAndIsDeletedFalse(String)` — 정확 일치 조회 (유사도 사전 검사용으로 예약됨)

**`SubscriptionRepository`**
- `existsByInterestIdAndUserId(UUID, UUID)` — 중복 구독 판정
- `findByInterestIdAndUserId(UUID, UUID)` — 구독 해제용 단건 조회
- `findAllByUserId(UUID)` — 사용자 구독 전체 (subscribedByMe 매핑용)
- `deleteAllByInterestId(UUID)` — Interest 삭제 시 일괄 정리

### 3-4. DTO

| DTO | 파일 | 필드 / 검증 |
|---|---|---|
| `InterestCreateRequest` | `dto/InterestCreateRequest.java` | `@NotBlank String name`, `@NotEmpty List<String> keywords` |
| `InterestUpdateRequest` | `dto/InterestUpdateRequest.java` | `@NotEmpty List<String> keywords` (이름은 포함되지 않음 = I3 이름 불변 규칙) |
| `InterestResponse` | `dto/InterestResponse.java` | `id, name, keywords, subscriberCount, subscribedByMe`. 정적 팩토리 `from(Interest, boolean)` (라인 16-24) |
| `SubscriptionResponse` | `dto/SubscriptionResponse.java` | `id, interestId, userId`. `from(Subscription)` |

### 3-5. 서비스 비즈 규칙

**`InterestService`** (`service/InterestService.java`)

| 메서드 | 규칙 |
|---|---|
| `create(InterestCreateRequest)` | 활성 목록 전체와 Levenshtein 유사도 비교 (임계값 0.8). 하나라도 ≥ 0.8 이면 `SimilarInterestNameException(details: {existing, similarity})` |
| `getInterests(keyword, sortBy, direction, userId)` | `ALLOWED_SORT_BY = {name, subscriberCount}`, `ALLOWED_DIRECTION = {asc, desc}`. 벗어나면 `InvalidSortParameterException`. `keyword`는 name 또는 keywords 중 하나와 부분일치. `userId != null`이면 구독 집합으로 `subscribedByMe` 매핑 |
| `updateKeywords(id, InterestUpdateRequest)` | soft-deleted 제외 조회, 미존재 시 `InterestNotFoundException`. keywords 전량 교체 |
| `delete(id)` | `markDeleted()` + `subscriptionRepository.deleteAllByInterestId(id)` |

**`InterestSubscriptionService`** (`service/InterestSubscriptionService.java`)

| 메서드 | 규칙 |
|---|---|
| `subscribe(interestId, userId)` | Interest 존재 확인 → 중복이면 `DuplicateSubscriptionException` → 저장 + `incrementSubscriberCount()` |
| `unsubscribe(interestId, userId)` | 미구독이면 `SubscriptionNotFoundException` → 삭제 + (Interest 존재 시) `decrementSubscriberCount()` |

### 3-6. 컨트롤러 엔드포인트

| Method | Path | 필수 헤더 | 응답 | 주요 예외 → Status |
|---|---|---|---|---|
| POST | `/api/interests` | `Monew-Request-User-ID` | 201 | SIMILAR_INTEREST_NAME → 409, INVALID_REQUEST(@Valid) → 400, MISSING_REQUEST_HEADER → 400 |
| GET | `/api/interests` | (선택, 있으면 subscribedByMe 채움) | 200 | INVALID_SORT_PARAMETER → 400 |
| PATCH | `/api/interests/{id}` | `Monew-Request-User-ID` | 200 | INTEREST_NOT_FOUND → 404, INVALID_REQUEST → 400 |
| DELETE | `/api/interests/{id}` | `Monew-Request-User-ID` | 204 | INTEREST_NOT_FOUND → 404 |
| POST | `/api/interests/{id}/subscriptions` | `Monew-Request-User-ID` | 201 | INTEREST_NOT_FOUND → 404, DUPLICATE_SUBSCRIPTION → 409 |
| DELETE | `/api/interests/{id}/subscriptions` | `Monew-Request-User-ID` | 204 | SUBSCRIPTION_NOT_FOUND → 404 |

GET 이외 모든 엔드포인트는 `@RequestHeader("Monew-Request-User-ID") UUID userId` 가 필수. 누락 시 `MissingRequestHeaderException` → 400 `MISSING_REQUEST_HEADER`.

### 3-7. 도메인 예외 6종

| 예외 | ErrorCode | 파일 |
|---|---|---|
| `InterestNotFoundException` | `INTEREST_NOT_FOUND` | `exception/InterestNotFoundException.java` |
| `SimilarInterestNameException` | `SIMILAR_INTEREST_NAME` | `exception/SimilarInterestNameException.java` |
| `InterestNameImmutableException` | `INTEREST_NAME_IMMUTABLE` | `exception/InterestNameImmutableException.java` (**미사용** — 7절 참조) |
| `InvalidSortParameterException` | `INVALID_SORT_PARAMETER` | `exception/InvalidSortParameterException.java` |
| `DuplicateSubscriptionException` | `DUPLICATE_SUBSCRIPTION` | `exception/DuplicateSubscriptionException.java` |
| `SubscriptionNotFoundException` | `SUBSCRIPTION_NOT_FOUND` | `exception/SubscriptionNotFoundException.java` |

### 3-8. 유틸

`src/main/java/com/example/monew/global/util/SimilarityUtils.java` (라인 1-34)
- `public static double similarity(String a, String b)` — Levenshtein 거리 기반 `1.0 - (distance / maxLen)`
- null 입력 시 `IllegalArgumentException`
- 빈 문자열 둘 다 / 동일 문자열 → 1.0

### 3-9. Interest 테스트 파일 맵

| 파일 | 테스트 개수 | 핵심 커버리지 |
|---|---|---|
| `entity/InterestTest.java` | 6 | 생성 불변식, replaceKeywords 교체, markDeleted, 카운터 증감 |
| `entity/SubscriptionTest.java` | 2 | 생성, null 방어 |
| `service/InterestServiceTest.java` | 11 | create/update/delete/list + 유사도 + keyword 부분일치 + sort |
| `service/InterestSubscriptionServiceTest.java` | 5 | subscribe 성공/미존재/중복, unsubscribe 성공/미존재 |
| `controller/InterestControllerTest.java` | 13 | 201/409/400/404/헤더누락 + keyword 파라미터 전달 |
| `controller/InterestSubscriptionControllerTest.java` | 7 | 201/204/404/409/헤더누락 |
| `repository/InterestRepositoryTest.java` | 3 | soft delete 필터링 전용 |
| `repository/SubscriptionRepositoryTest.java` | 3 | 중복 판정 / 조회 / 일괄 삭제 |
| `exception/InterestExceptionsTest.java` | 6 | 각 예외 → ErrorCode/details 매핑 |
| `InterestApiIntegrationTest.java` | 5 | E2E fullFlow, 유사 409, sort 400, 헤더누락 400, 구독 미존재 404 |

---

## 4. TDD 5단계 사이클 규약

### 4-1. 사이클 정의

| 단계 | 커밋 prefix | 내용 | Red 검증 방법 |
|---|---|---|---|
| 1 | `[Red-Unit]` | 실패하는 **단위** 테스트 1건 추가 | `./gradlew test --tests <FQN>` 로 해당 테스트만 실행 → 실패 확인 |
| 2 | `[Green-Unit]` | 단위 테스트를 통과시키는 **최소** 프로덕션 코드 | 해당 단위 테스트 통과 |
| 3 | `[Red-Integration]` | 실패하는 **슬라이스/통합** 테스트 (`@WebMvcTest` 또는 `@SpringBootTest`) | 통합 테스트 실행 → 실패 확인 |
| 4 | `[Green-Integration]` | 컨트롤러 매핑·핸들러·와이어링 최소 추가 | 통합 테스트 통과 |
| 5 | `[Refactor]` | 동작 변경 없는 정리 (네이밍/추상화/중복 제거). 필요 없으면 **생략** | 모든 테스트 그린 유지 |

단위 전용 항목(enum 추가, record 생성 등)은 1~2단계만 수행하고 3~4는 N/A.

### 4-2. 커밋 메시지 형식

**제목** (1줄, 50자 이내):
```
[단계] [MON-XX] 간결 설명
```
예: `[Red-Unit] [MON-43] InterestService.create 유사도 차단 단위`

**본문**:
```
- 변경한 내용:
  * 무엇을 수정/추가/삭제했는지 (구체적 파일/메서드 명)

- 변경 이유:
  * 이 변경이 왜 필요했는지 (티켓/위키 ID 참조)

- 주요 파일:
  * 핵심 변경 파일 경로 2~5개

- 검증:
  * 어떤 테스트로 / 어떤 명령으로 확인했는지
```

**금지**:
- `Co-Authored-By: Claude`, Claude 흔적이 드러나는 모든 표현
- `CLAUDE.md`, `.claude/` 파일 스테이징
- 날짜 기반 제목 (`2026-04-15 커밋` 등)
- PR 제목에 `[MON-27~58]` 같은 범위 표기 (Jira 연동 끊김)

---

## 5. 작업 워크플로 체크리스트

후임자가 새 기능 하나를 추가할 때 순서대로 따라간다.

1. Jira에서 티켓 `MON-XX` 확인 — 요구사항 명세를 위키 기준으로 재확인
2. 브랜치 분기: `git checkout -b feat/MON-XX-간결설명` (base: 인프라 머지 전이면 `feat/exception-infra`, 이후에는 `develop`)
3. **[Red-Unit]** 단위 테스트 작성 → `./gradlew test --tests <FQN>` 로 실패 확인 → 커밋
4. **[Green-Unit]** 최소 구현 → 단위 테스트 통과 → 커밋
5. **[Red-Integration]** 슬라이스(`@WebMvcTest`) 또는 E2E(`@SpringBootTest`) 케이스 추가 → 실패 확인 → 커밋
6. **[Green-Integration]** 컨트롤러/핸들러/와이어링 추가 → 통합 테스트 통과 → 커밋
7. (선택) **[Refactor]** 중복 제거·이름 정리 → 전체 테스트 그린 → 커밋
8. 풀 회귀 `./gradlew test` 전부 그린 확인
9. PR 생성:
   - 제목: `[MON-XX] 간결 설명` (단일 키)
   - 본문 첫 줄: `**관련 티켓**: MON-XX` (복수면 `MON-27 MON-28 ...` 공백 구분)
   - 본문에 위키 검증 매핑(V? / TS-?? / I?) 명시
10. base 브랜치가 `develop`이 아닌 Stacked PR이라면 PR 본문에 base를 명시

---

## 6. 테스트 템플릿 (복사해서 시작하면 된다)

### 6-1. `@WebMvcTest` 컨트롤러 슬라이스

```java
@WebMvcTest(controllers = InterestController.class)
@Import(GlobalException.class)
class InterestControllerTest {
  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;
  @MockitoBean InterestService interestService;
  // ... when(...).thenReturn(...) + mockMvc.perform(...) + andExpect(...)
}
```

핵심: `@Import(GlobalException.class)` 가 빠지면 예외 핸들러가 잡히지 않아 500만 떨어진다. 반드시 포함.

### 6-2. `@DataJpaTest` 레포지토리

```java
@DataJpaTest
class InterestRepositoryTest {
  @Autowired InterestRepository repository;
  // entityManager 대신 repository.save() 로 준비
}
```

`InterestKeyword.value` 를 커스텀 컬럼명(`keyword_value`)으로 매핑해 둔 이유는 H2가 `value`를 예약어로 처리하기 때문. 새 엔티티 추가 시 예약어 체크 필수.

### 6-3. `@SpringBootTest` E2E

```java
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class InterestApiIntegrationTest { ... }
```

`InterestApiIntegrationTest.fullFlow` (I1→I5→I2→I6→I2→I3→I4) 를 템플릿으로 참고.

---

## 7. 미구현 / 보강 항목 — 후임자가 이어갈 작업

| # | 항목 | 근거 | 제안 설계 | 첫 번째 Red 테스트 |
|---|---|---|---|---|
| 1 | `InterestMapper` 분리 | 유지보수성 (현재 `mapper/` 디렉토리는 비어있음) | `@Component InterestMapper` 에 `toResponse(Interest, boolean)`, 나아가 `toEntity(InterestCreateRequest)`. 기존 `InterestResponse.from` 호출부를 매퍼 주입으로 치환 | `mapper/InterestMapperTest#toResponseMapsSubscribedByMe` |
| 2 | I2 커서 페이징 | 모뉴-기능-요구사항 I2 ("키워드 검색, cursor 페이징") | `record InterestCursor(String sortKey, UUID lastId)` + `record CursorSlice<T>(List<T> content, String nextCursor, boolean hasNext)` + `InterestSpecifications` (Spring Data Specification). `getInterests` 시그니처 확장 또는 `getInterestsSlice`로 분리 | `InterestServiceTest#getInterestsCursorNextPage` (2페이지 경계 검증) |
| 3 | Swagger / OpenAPI (TS-10) | 모뉴-스웨거-테스트-플랜 TS-10 | `build.gradle`에 `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0` 추가 → 컨트롤러 6개 엔드포인트에 `@Tag`, `@Operation`, `@ApiResponses`(+ ErrorResponse 예시) | `SwaggerEndpointSmokeTest#openApiJsonAvailable` (`/v3/api-docs` 200 확인) |
| 4 | 기능별 `[Refactor]` 커밋 | TDD 5단계 규율 (현재 I1~I6 대부분 Refactor 커밋 없음) | 공통 Comparator 팩토리, 검증 메시지 상수, 스트림 체이닝 정리. **동작 변경 금지** | 기존 테스트 전부 그린 유지 확인 |
| 5 | JaCoCo 80% 커버리지 (V10) | 모뉴-기술-요구사항 V10 | `build.gradle` 에 `jacoco` 플러그인 + `jacocoTestReport` 의존 + `jacocoTestCoverageVerification` 룰(`LINE ≥ 0.80`). CI에서 리포트 업로드 | CI 리포트에서 Interest/Global 패키지 라인 커버리지 ≥ 80% 확인 |
| 6 | `InterestNameImmutableException` 활용 | 위키 I3 ("키워드만 수정") — 현재 선언만 있고 사용처 없음 | PATCH 요청 본문에 `name` 필드가 섞여 들어오면 방어적으로 거부. `InterestUpdateRequest`에 `name` 필드 추가 여부 / 핸들러 방어 둘 중 택일 | `InterestControllerTest#patchWithNameFieldRejected400` |
| 7 | `InterestRepository.findByNameAndIsDeletedFalse` 활용 | 현재 선언만 있고 호출 없음 | 유사도 검사 전에 정확 일치 중복을 먼저 차단해 비용 절감 | `InterestServiceTest#createExactNameDuplicateRejected` |

### 7-1. Swagger 복원 시 주의

과거 커밋 히스토리에 `springdoc-openapi-starter-webmvc-ui:2.6.0` 이 존재했으나 (abandoned `backup-pre-jira-rewrite` 브랜치의 Phase 0 커밋 `a7fdd6d`), 현재 `build.gradle`에서는 제거된 상태다. 복원 시 `spring-boot-starter-web`과 버전 호환만 맞추면 된다. `/swagger-ui.html` 대신 `/swagger-ui/index.html` 접근이 기본.

---

## 8. 트러블슈팅

| 증상 | 원인 | 해결 |
|---|---|---|
| `@WebMvcTest`에서 예외를 던졌는데 500만 떨어짐 | `@Import(GlobalException.class)` 누락 | 테스트 클래스에 `@Import(GlobalException.class)` 추가 |
| H2에서 `InterestKeyword` INSERT가 syntax error | `value` 는 H2 예약어 | 이미 `@Column(name = "keyword_value")` 매핑됨. 새 엔티티에서 `value`, `user`, `order` 등 예약어 사용 금지 |
| PR이 Jira 티켓에 안 붙음 | 제목에 `[MON-27~58]` 범위 표기 / 여러 키 나열 | 제목은 단일 `[MON-27]`, 본문 첫 줄 `**관련 티켓**: MON-27 MON-28 ...` |
| Stacked PR rebase 충돌 | 인프라 머지 후 Interest가 오래된 base 물고 있음 | `git fetch upstream && git rebase upstream/develop && git push --force-with-lease` |
| 테스트에서 `MDC.get("request_id")` 가 null | MDC 세터가 없는 테스트 컨텍스트 | `ErrorResponse.traceId`는 null 허용. 필요 시 `TestMdcInterceptor` 추가 |
| `./gradlew bootRun` DB 연결 실패 | 로컬 env에 `DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD` 미설정 | `.env` 설정 또는 `-Dspring.profiles.active=test` + H2 프로파일 사용 |

---

## 9. 부록

### 9-1. 위키 검증 매트릭스

| 위키 ID | 명세 요지 | 현재 구현 위치 |
|---|---|---|
| V1 (기술 요구사항:143) | 인증 헤더 누락 → 400/401 | `GlobalException.handleMissingRequestHeader` + I1/I3/I4/I5/I6 컨트롤러 테스트 |
| V2 (기술 요구사항:144) | Bean Validation 실패 → 400 + fieldErrors | `GlobalException.handleValidationException` + `GlobalExceptionWebMvcTest` @Valid 케이스 |
| V3 (기술 요구사항:145) | 커스텀 예외 응답 일관성 | `ErrorResponse.of(MonewException)` + 도메인 예외 6종 |
| V4 (기술 요구사항:146) | 80% Levenshtein 유사 차단 | `InterestService.create` + `SimilarityUtils` |
| V10 | 라인 커버리지 80% | **미구현** (7절 #5) |
| TS-02:2 (스웨거:76) | SIMILAR_INTEREST_NAME 409 | `InterestServiceTest#createSimilarRejected` + `InterestControllerTest#createSimilar409` |
| TS-03:2 | DUPLICATE_SUBSCRIPTION 409 | `InterestSubscriptionServiceTest#subscribeDuplicate` + 컨트롤러 테스트 |
| TS-03:4 | SUBSCRIPTION_NOT_FOUND 404 | `InterestSubscriptionServiceTest#unsubscribeNotFound` + 컨트롤러 테스트 |
| TS-08 | 공통 에러 처리 + MDC | `GlobalExceptionWebMvcTest` 전 7건 + `application.yaml` 로깅 패턴 |
| TS-10 | Swagger / OpenAPI | **미구현** (7절 #3) |
| I1~I6 (기능 요구사항:46-51) | 관심사 CRUD + 구독 풀 워크플로 | `InterestApiIntegrationTest#fullFlow` |

### 9-2. 헤더 / MDC 스펙

- 요청 헤더 `Monew-Request-User-ID` (UUID) — GET `/api/interests` 를 제외한 모든 Interest 엔드포인트에서 필수
- MDC 키: `request_id`, `client_ip` — `application.yaml:25-27` 로그 패턴에서 읽음
- `ErrorResponse.traceId` 는 `MDC.get("request_id")` 로 주입 (라인 29, 39, 55, 67)

### 9-3. 유용한 명령 모음

```bash
# 단일 테스트만 (Red 검증)
./gradlew test --tests "com.example.monew.domain.interest.service.InterestServiceTest.createSimilarRejected"

# 패키지 전체
./gradlew test --tests "com.example.monew.domain.interest.*"

# 풀 회귀
./gradlew test

# 부팅 확인
./gradlew bootRun
```

---

## 10. 마지막으로

- 이 문서는 "지금 이 상태에서 이어받는" 사람을 위한 지도다. 구현이 완전하지 않은 부분(7절)은 설계안과 Red 테스트 제안이 포함되어 있으므로, 그 칸의 테스트 한 줄을 쓰는 것부터 시작하면 된다.
- 규칙이 바뀌었거나 도메인이 성장한 경우, 이 문서도 해당 PR에서 함께 업데이트한다 — 문서-코드 드리프트를 막는다.
- 위키 골든 소스와 본 문서가 충돌할 경우, 최우선은 **모뉴 노션 문서**이다. 본 문서는 현재 코드와 노션문서를 잇는 요약일 뿐이다.
