-- UUID 생성을 위한 확장 모듈 설치 (최초 1회)


-- 1. 사용자 테이블 (Users)
CREATE TABLE users
(
    id         UUID PRIMARY KEY,
    email      VARCHAR(255) UNIQUE NOT NULL,
    nickname   VARCHAR(20)         NOT NULL,
    password   VARCHAR(255)        NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITHOUT TIME ZONE
);
-- 2. 관심사 테이블 (Interests) - 추가됨
CREATE TABLE interests
(
    id               UUID PRIMARY KEY,
    name             VARCHAR(50) UNIQUE NOT NULL,
    keywords         VARCHAR(50)[]      NOT NULL,
    subscriber_count BIGINT                      DEFAULT 0,
    created_at       TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at       TIMESTAMP WITHOUT TIME ZONE -- 요구사항: 논리 삭제
);

-- 3. 구독 테이블 (Subscriptions) - 추가됨 (사용자-관심사 N:M 매핑)
CREATE TABLE subscriptions
(
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL,
    interest_id UUID NOT NULL,
    created_at  TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,

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
    publish_date  TIMESTAMP WITHOUT TIME ZONE,
    created_at    TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at    TIMESTAMP WITHOUT TIME ZONE
);

-- 5. 뉴스 기사 조회 내역 테이블 (Article_Views) - 추가됨 (조회수 중복 방지 및 활동 내역용)
CREATE TABLE article_views
(
    id         UUID PRIMARY KEY,
    article_id UUID NOT NULL,
    user_id    UUID NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,

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
    deleted_at TIMESTAMP WITHOUT TIME ZONE,

    CONSTRAINT fk_comment_article FOREIGN KEY (article_id) REFERENCES articles (id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- 7. 댓글 좋아요 테이블 (Comment_Likes) - 추가됨 (좋아요 중복 방지 및 활동 내역용)
CREATE TABLE comment_likes
(
    id         UUID PRIMARY KEY,
    comment_id UUID NOT NULL,
    user_id    UUID NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_like_comment FOREIGN KEY (comment_id) REFERENCES comments (id) ON DELETE CASCADE,
    CONSTRAINT fk_like_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_comment_user_like UNIQUE (comment_id, user_id) -- 중복 좋아요 방지
);

-- 8. 알림 테이블 (Notifications)
CREATE TABLE notifications
(
    id            UUID PRIMARY KEY,
    user_id       UUID NOT NULL,
    content       TEXT NOT NULL,
    resource_type VARCHAR(50),
    resource_id   UUID,
    confirmed     BOOLEAN                     DEFAULT FALSE,
    created_at    TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_noti_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);