# 모뉴(MoNew) 예외 처리 가이드

> 모든 도메인 담당자가 5분 안에 따라 할 수 있는 실용 가이드.
> 인프라 위치: `com.example.monew.global.exception`
> - `ErrorCode` (enum, status + message)
> - `MonewException` (abstract, 도메인 서브클래스가 상속)
> - `GlobalExceptionHandler` (`@RestControllerAdvice`)
> - `ErrorResponse` (응답 DTO)

---

## 1. 에러 응답 표준 형식

모든 예외는 `ErrorResponse` 형태의 JSON으로 응답한다.

```json
{
  "timestamp": "2026-04-15T10:09:50.123Z",
  "code": "USER_NOT_FOUND",
  "message": "해당 유저를 찾을 수 없습니다.",
  "details": { "userId": "..." },
  "exceptionType": "UserNotFoundException",
  "status": 404,
  "traceId": "abc-123"
}
```

| 필드 | 의미 |
|------|------|
| `timestamp` | 발생 시각 (Instant, UTC) |
| `code` | `ErrorCode` enum 이름 (예: `DUPLICATE_EMAIL`) |
| `message` | `ErrorCode`에 정의된 한글 메시지 |
| `details` | 컨텍스트(예: 충돌 필드, 검증 오류) — 없을 수 있음 |
| `exceptionType` | 예외 클래스 SimpleName |
| `status` | HTTP 상태 코드 (int) |
| `traceId` | MDC `request_id` (요청 추적용) |

---

## 2. 새 도메인 예외 추가 4단계

### Step 1. `ErrorCode`에 코드 추가

`src/main/java/com/example/monew/global/exception/ErrorCode.java`

```java
// Comment
COMMENT_NOT_FOUND(NOT_FOUND, "댓글을 찾을 수 없습니다."),
```

- 도메인 단위로 그룹핑(주석으로 구분)
- 상태 코드는 `org.springframework.http.HttpStatus` static import

### Step 2. 도메인 예외 클래스 작성

`src/main/java/com/example/monew/domain/comment/exception/CommentNotFoundException.java`

```java
package com.example.monew.domain.comment.exception;

import com.example.monew.global.exception.ErrorCode;
import com.example.monew.global.exception.MonewException;
import java.util.Map;
import java.util.UUID;

public class CommentNotFoundException extends MonewException {
  public CommentNotFoundException(UUID commentId) {
    super(ErrorCode.COMMENT_NOT_FOUND, Map.of("commentId", commentId));
  }
}
```

- `MonewException` 상속 (abstract라 직접 인스턴스화 불가)
- 생성자에서 `details`에 식별자/컨텍스트를 채워 디버깅성 확보

### Step 3. 서비스에서 throw

```java
return commentRepository.findById(id)
    .orElseThrow(() -> new CommentNotFoundException(id));
```

- **try-catch로 감싸서 다른 응답으로 변환하지 말 것** — Handler에 위임
- 외부 시스템 실패는 도메인 예외로 래핑 (`new ArticleFetchFailedException(cause)`)

### Step 4. 컨트롤러에 응답 명세 (Swagger)

```java
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "성공"),
    @ApiResponse(responseCode = "404",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
@GetMapping("/{id}")
public CommentDto get(@PathVariable UUID id) { ... }
```

---

## 3. 명명 규칙

| HTTP | 패턴 | 예 |
|------|------|---|
| 404 | `Xxx**NotFound**Exception` | `UserNotFoundException` |
| 409 | `**Duplicate**XxxException` / `Xxx**AlreadyExists**Exception` | `DuplicateEmailException` |
| 403 | `Xxx**Forbidden**Exception` | `CommentForbiddenException` |
| 401 | `**Invalid**CredentialsException`, `Unauthorized...` | `InvalidCredentialsException` |
| 400 | `**Invalid**XxxException` | `InvalidSortParameterException` |
| 502 | `Xxx**FetchFailed**Exception` | `ArticleFetchFailedException` |

`ErrorCode` 명은 `도메인_상태` (예: `COMMENT_FORBIDDEN`, `SIMILAR_INTEREST_NAME`).

---

## 4. 예외 vs 검증 사용 기준

| 상황 | 처리 방법 |
|------|----------|
| DTO 필드 형식/길이 검증 | `@Valid` + Jakarta Validation (`@NotBlank`, `@Email` 등) → `MethodArgumentNotValidException` 자동 핸들러 |
| PathVariable / RequestParam 검증 | `@Validated` 컨트롤러 + `@Min`/`@Pattern` → `ConstraintViolationException` 자동 핸들러 |
| 비즈니스 규칙 위반 (중복, 권한, 상태) | 도메인 예외 throw |
| 외부 시스템(HTTP, DB) 실패 | catch → 도메인 예외로 래핑해서 throw |
| 필수 요청 헤더 누락 (`MoNew-Request-User-ID`) | `@RequestHeader` 자동 → `MissingRequestHeaderException` 자동 핸들러 |

---

## 5. 표준 핸들러 매핑 (이미 처리됨)

`GlobalExceptionHandler`에서 자동 처리되는 예외 → 도메인 코드에서 별도 catch 불필요.

| 예외 | 응답 코드 | 응답 ErrorCode |
|------|----------|---------------|
| `MonewException` (서브클래스) | `ErrorCode.status` | 자기 자신 |
| `MethodArgumentNotValidException` | 400 | `INVALID_REQUEST` |
| `ConstraintViolationException` | 400 | `CONSTRAINT_VIOLATION` |
| `MissingRequestHeaderException` | 400 | `MISSING_REQUEST_HEADER` |
| `HttpRequestMethodNotSupportedException` | 405 | `METHOD_NOT_ALLOWED_REQUEST` |
| `HttpMediaTypeNotSupportedException` | 415 | `UNSUPPORTED_MEDIA_TYPE_REQUEST` |
| `HttpMessageNotReadableException` | 400 | `MALFORMED_REQUEST_BODY` |
| `NoHandlerFoundException` | 404 | `RESOURCE_NOT_FOUND` |
| `IllegalArgumentException` | 400 | `INVALID_REQUEST` |
| `Exception` (fallback) | 500 | `INTERNAL_ERROR` |

---

## 6. 테스트 패턴

### 6-1. 도메인 예외 단위 테스트 (서비스)

```java
@Test
void 존재하지_않는_댓글_조회시_예외() {
  given(commentRepository.findById(id)).willReturn(Optional.empty());

  assertThatThrownBy(() -> service.get(id))
      .isInstanceOf(CommentNotFoundException.class)
      .extracting("errorCode").isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
}
```

### 6-2. 컨트롤러 → ErrorResponse 통합 테스트 (MockMvc)

```java
@Test
void 존재하지_않는_댓글_404_응답() throws Exception {
  given(service.get(id)).willThrow(new CommentNotFoundException(id));

  mockMvc.perform(get("/api/comments/{id}", id))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"))
      .andExpect(jsonPath("$.status").value(404));
}
```

핸들러 자체 검증은 `src/test/java/com/example/monew/global/exception/GlobalExceptionHandlerTest.java` 참고.

---

## 7. 자주 하는 실수

- ❌ 서비스에서 `throw new MonewException(...)` 직접 호출 — abstract라 컴파일 오류. 도메인 예외 클래스를 만들 것.
- ❌ 컨트롤러에서 `try { ... } catch (Exception e) { return ResponseEntity.status(500)... }` — 핸들러를 우회하면 응답 일관성이 깨짐.
- ❌ 도메인 예외에 한국어 메시지를 따로 넘김 — 메시지는 `ErrorCode`에서만 정의(국제화/일관성).
- ❌ `details`에 민감 정보(비밀번호, 토큰 등) 포함 — 식별자/검증 필드만.
- ❌ 새 ErrorCode 추가 시 HttpStatus 누락 — enum 생성자가 강제하므로 컴파일 오류로 감지됨.

---

## 8. 참고

- 핸들러 본체: `src/main/java/com/example/monew/global/exception/GlobalExceptionHandler.java`
- 핸들러 단위 테스트: `src/test/java/com/example/monew/global/exception/GlobalExceptionHandlerTest.java`
- 응답 추적 ID: `ErrorResponse.traceId`는 MDC `request_id`를 읽음. **현재 MDC를 채우는 필터/인터셉터가 미구현 상태이므로 항상 `null`로 응답됨.** 추적 ID 미들웨어가 별도 PR로 도입되면 자동 채워짐(로그 패턴 `%X{request_id}`도 동일 키 사용).
