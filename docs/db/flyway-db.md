# 🚀 MoNew DB 형상 관리 가이드 (Flyway)

우리 프로젝트는 DB 스키마 동기화 및 버전 관리를 위해 **Flyway**를 사용합니다.
이제 모든 팀원이 동일한 DB 구조를 유지하며 효율적으로 협업할 수 있습니다!

---

## 🛠️ 최초 1회 세팅 방법 (필수)
이미 로컬 DB에 테이블이 있는 경우 Flyway와 충돌이 납니다. 딱 한 번만 밀어주세요.

1. **Gradle 갱신**: IntelliJ 우측 Gradle 탭에서 **코끼리 아이콘(Reload All Gradle Projects)** 클릭
2. **테이블 삭제**: DataGrip 등에서 `public` 스키마 안의 **모든 테이블**을 `Drop`
    - `flyway_schema_history` 테이블이 있다면 그것까지 포함해서 삭제!
3. **서버 실행**: 애플리케이션을 Run 하면 `V1__init.sql`이 자동 실행됩니다.

---

## 🤝 협업 규칙 (약속!)

### 1. DB 수정은 오직 SQL 파일로만!
- 직접 DB GUI(DataGrip 등)에서 테이블/컬럼을 만들지 마세요.
- 반드시 `src/main/resources/db/migration/` 경로에 새 파일을 만듭니다.
- **파일명 규칙**: `V[버전]__[설명].sql` (언더바는 반드시 **2개**여야 합니다.)
    - 예: `V2__add_column_to_users.sql`

### 2. 이미 Push된 파일은 수정 금지 (중요)
- 이미 공유된 파일을 수정하면 **Checksum mismatch** 에러가 발생하여 팀 전원의 서버가 뜨지 않습니다.
- 수정 사항이 생기면 무조건 다음 버전(`V2`, `V3`...) 파일을 새로 만드세요.

### 3. 버전 충돌 방지
- 여러 명이 동시에 작업할 땐 파일명에 날짜/시간을 붙여주세요.
    - 예: `V202604271815__create_article_table.sql`

---

## 🆘 자주 발생하는 에러 해결

| 에러 메시지 | 원인 | 해결 방법 |
| :--- | :--- | :--- |
| **Checksum mismatch** | 실행된 SQL 파일 내용을 수정함 | 수정한 내용 원복 또는 `history` 테이블 삭제 |
| **Schema-validation fail** | Entity와 DB 컬럼명이 다름 | 자바 코드(@Entity)와 SQL 컬럼명 대조 |
| **Duplicate version** | 팀원과 동일한 버전 번호 사용 | 버전 번호를 겹치지 않게 수정 |

---

## ✅ 성공 확인
서버 실행 후 DB에 **`flyway_schema_history`** 테이블이 생겼다면 성공입니다!