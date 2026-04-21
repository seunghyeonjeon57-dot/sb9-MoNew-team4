# MoNew Postman 자동화 테스트

관심사(Interest) 도메인 Postman Collection. **Self-contained** — 외부 사전 준비 없이 Newman 단독으로 전체 시나리오가 순차 실행된다.

MON-112 Swagger 계약 정렬 + MON-122 예외 번역 반영 기준.

## 파일

| 파일 | 용도 |
|---|---|
| `MoNew-Interest.postman_collection.json` | 관심사 API 23개 시나리오 (자동 assertion 포함) |
| `MoNew-Interest.postman_environment.json` | 로컬 환경 변수 (`baseUrl`만 보유) |

## 사용법

### 1. Postman GUI

1. Postman 실행 → **Import** 클릭
2. 두 파일 동시 드래그 앤 드롭
3. 우측 상단 환경 선택기에서 **MoNew Interest Local** 선택
4. 앱 기동: `./gradlew bootRun --args='--spring.profiles.active=dev'`
5. Collection 우클릭 → **Run collection** → **Run MoNew Interest API**

### 2. Newman CLI (자동화 / CI)

```bash
# 설치 (최초 1회)
npm install -g newman

# 실행 — 환경 파일 생략해도 동일 동작 (컬렉션에 baseUrl 기본값 포함)
newman run postman/MoNew-Interest.postman_collection.json

# 또는 환경 파일과 함께
newman run postman/MoNew-Interest.postman_collection.json \
  -e postman/MoNew-Interest.postman_environment.json
```

HTML 리포트를 원하면 `newman-reporter-html`을 추가 설치 후 `--reporters cli,html --reporter-html-export newman-report.html` 옵션을 붙인다.

## 시나리오 구성 (23개)

실행 순서 의존성이 있으므로 반드시 순차 실행.

| # | 시나리오 | 기대 |
|---|---|---|
| 00a | POST /api/users 테스트 유저 등록 (setup) | 201 |
| 00b | POST /api/users/login 로그인 (userId 자동 저장) | 200 + UserDto |
| 01 | POST /api/interests 등록 | 201 + InterestDto |
| 02 | POST 정확 일치 중복 | 409 similarity=1.0 |
| 03 | POST 유사 이름 | 409 SIMILAR_INTEREST_NAME |
| 04 | POST 헤더 누락 | 400 MISSING_REQUEST_HEADER |
| 05 | POST name 51자 | 400 INVALID_REQUEST |
| 06 | POST keywords 11개 | 400 INVALID_REQUEST |
| 07 | GET 필수 파라미터 | 200 + nextAfter 필드 |
| 08 | GET orderBy 누락 | 400 INVALID_REQUEST |
| 09 | GET 헤더 누락 | 400 MISSING_REQUEST_HEADER |
| 10 | GET orderBy=foo | 400 INVALID_SORT_PARAMETER |
| 11 | GET direction=asc(소문자) | 400 INVALID_SORT_PARAMETER |
| 12 | POST 구독 | 200 + SubscriptionDto 6필드 |
| 13 | POST 중복 구독 | 409 DUPLICATE_SUBSCRIPTION |
| 14 | GET subscribedByMe=true | 200 + count 증가 |
| 15 | PATCH name 포함 | 400 INTEREST_NAME_IMMUTABLE |
| 16 | PATCH keywords만 | 200 + 갱신 |
| 17 | DELETE 구독 취소 | 200 |
| 18 | DELETE 재시도 | 404 SUBSCRIPTION_NOT_FOUND |
| 19 | DELETE 관심사(물리) | 204 |
| 20 | PATCH 삭제된 관심사 | 404 INTEREST_NOT_FOUND |
| 21 | POST 미존재 관심사 구독 | 404 INTEREST_NOT_FOUND |

## 변수 동작

- `runSuffix`: 00a의 pre-request에서 현재 timestamp로 1회 세팅 → 이후 모든 요청에서 공유 (이메일·이름 중복 회피)
- `userId`: 00b 응답의 `UserDto.id`에서 자동 추출
- `interestId`: 01 응답의 `id`에서 자동 저장
- `baseUrl`: 컬렉션 기본값 `http://localhost:8080`. 환경 파일로 오버라이드 가능

## 배포 서버 대상 테스트 시

환경 변수 `baseUrl`을 아래로 변경 또는 `--env-var "baseUrl=..."` 옵션 사용:
```
http://sprint-project-1196140422.ap-northeast-2.elb.amazonaws.com/sb/monew
```
주의: HTTPS 미지원. `http://` 그대로 사용.

## 의존 PR

- MON-122(#67) — 구독 FK 위반 번역 수정. 미머지 상태에서는 13번(중복 구독) 시나리오 대신 FK 위반이 `DUPLICATE_SUBSCRIPTION`로 잘못 매핑되어 결과가 우연히 맞을 수 있음. **머지 이후에만 전수 녹색 보장**.
- MON-123(#68) — OpenAPI 어노테이션 보강.

## 계약 기준점

- **Swagger**: `docs/monew-swagger.md` / `.json`
- **구현**: MON-112 (`feat/MON-112-interest-swagger-alignment`), MON-122 (`fix/MON-122-subscription-exception-translation`)
- **응답 포맷**: `ErrorResponse(code, message, details, timestamp)` — `src/main/java/com/example/monew/global/exception/ErrorResponse.java`
