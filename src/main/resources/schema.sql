-- UUID 생성을 위한 확장 모듈 설치
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. 사용자 테이블 (Users)
CREATE TABLE users
(
    id         UUID PRIMARY KEY,
    email      VARCHAR(255) UNIQUE NOT NULL,
    nickname   VARCHAR(20)         NOT NULL,
    password   VARCHAR(255)        NOT NULL,
    status     VARCHAR(20)         NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITHOUT TIME ZONE
);

-- 2. 관심사 테이블 (Interests)
CREATE TABLE interests
(
    id               UUID PRIMARY KEY,
    name             VARCHAR(50) UNIQUE NOT NULL,
    subscriber_count BIGINT                      DEFAULT 0,
    created_at       TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at       TIMESTAMP WITHOUT TIME ZONE
);

-- 2-1. 관심사 키워드 테이블 (Interest_Keywords)
-- 키워드는 별도의 BaseEntity 상속 여부에 따라 컬럼을 조절하세요.
-- (보통 키워드 단건은 수정/삭제 이력이 필요 없으므로 기본 구조만 유지합니다)
CREATE TABLE interest_keywords
(
    id            UUID PRIMARY KEY,
    interest_id   UUID        NOT NULL,
    keyword_value VARCHAR(50) NOT NULL,
    CONSTRAINT fk_keyword_interest FOREIGN KEY (interest_id) REFERENCES interests (id) ON DELETE CASCADE
);

-- 3. 구독 테이블 (Subscriptions)
CREATE TABLE subscriptions
(
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL,
    interest_id UUID NOT NULL,
    created_at  TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT fk_sub_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_sub_interest FOREIGN KEY (interest_id) REFERENCES interests (id) ON DELETE CASCADE,
    CONSTRAINT uk_user_interest UNIQUE (user_id, interest_id)
);

-- 4. 뉴스 기사 테이블 (Articles)
CREATE TABLE articles
(
    id            UUID PRIMARY KEY,
    source        VARCHAR(100),
    source_url    TEXT UNIQUE  NOT NULL,
    title         VARCHAR(255) NOT NULL,
    summary       TEXT,
    view_count    BIGINT                      DEFAULT 0,
    comment_count BIGINT                      DEFAULT 0,
    interest      VARCHAR(255),
    publish_date  TIMESTAMP WITHOUT TIME ZONE,
    created_at    TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at    TIMESTAMP WITHOUT TIME ZONE
);

-- 5. 뉴스 기사 조회 내역 테이블 (Article_Views)
CREATE TABLE article_views
(
    id         UUID PRIMARY KEY,
    article_id UUID NOT NULL,
    user_id    UUID NOT NULL,
    client_ip  VARCHAR(45),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT fk_view_article FOREIGN KEY (article_id) REFERENCES articles (id) ON DELETE CASCADE,
    CONSTRAINT fk_view_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_article_user_view UNIQUE (article_id, user_id)
);

-- 6. 댓글 테이블 (Comments)
CREATE TABLE comments
(
    id         UUID PRIMARY KEY,
    article_id UUID         NOT NULL,
    user_id    UUID         NOT NULL,
    content    VARCHAR(500) NOT NULL,
    like_count BIGINT                      DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT fk_comment_article FOREIGN KEY (article_id) REFERENCES articles (id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- 7. 댓글 좋아요 테이블 (Comment_Likes)
CREATE TABLE comment_likes
(
    id         UUID PRIMARY KEY,
    comment_id UUID NOT NULL,
    user_id    UUID NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT fk_like_comment FOREIGN KEY (comment_id) REFERENCES comments (id) ON DELETE CASCADE,
    CONSTRAINT fk_like_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_comment_user_like UNIQUE (comment_id, user_id)
);

-- 8. 알림 테이블 (Notifications)
CREATE TABLE notifications
(
    id            UUID PRIMARY KEY,
    user_id       UUID NOT NULL,
    content       TEXT NOT NULL,
    resource_type VARCHAR(50),
    resource_id   UUID,
    is_confirmed  BOOLEAN                     DEFAULT FALSE,
    created_at    TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at    TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT fk_noti_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- 인덱스 설정
CREATE INDEX idx_users_status_deleted_at ON users (status, deleted_at);
CREATE INDEX idx_articles_publish_date ON articles (publish_date);
CREATE INDEX idx_comments_article_id ON comments (article_id);