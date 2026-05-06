# 🚀 MoNew (sb9-MoNew-team4)

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=seunghyeonjeon57-dot_sb9-MoNew-team4&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=seunghyeonjeon57-dot_sb9-MoNew-team4)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=seunghyeonjeon57-dot_sb9-MoNew-team4&metric=coverage)](https://sonarcloud.io/summary/new_code?id=seunghyeonjeon57-dot_sb9-MoNew-team4)

> **MoNew**는 사용자가 원하는 키워드만 설정하면 흩어져 있는
최신 뉴스를 자동으로 수집하여 맞춤형 피드로 제공합니다.
> 하이버네이트의 글로벌 설정을 우회하는 트러블 슈팅 경험과 **93.2%의 높은 테스트 커버리지**를 통해 서비스의 안정성을 확보했습니다.

---

### 🛠️ Tech Stack
- **Language & Framework**: **Java 17**, **Spring Boot 3.x**
- **Persistence**: **Spring Data JPA**, **Querydsl**, **Flyway**, **PostgreSQL**, **MongoDB**
- **Logic & Batch**: **Spring Batch**
- **Infrastructure**: **AWS ECS (Fargate)**, ECR, S3, RDS, **Docker**
- **Quality Control**: **SonarCloud**, GitHub Actions

### 📈 Test Strategy & Coverage
- **Coverage**: **93.2%** (SonarCloud 기준)
- **Focus**: 비즈니스 로직의 정합성을 보장하기 위해 Service 및 Repository 레이어에 대한 단위 테스트와 통합 테스트를 병행했습니다.
- **Data Integrity**: **Flyway**를 활용하여 데이터베이스 스키마 마이그레이션을 버전별로 관리하고, 테스트 환경에서의 정합성을 유지했습니다.