-- 1. Users Table
create table public.users
(
    id         uuid         not null primary key,
    email      varchar(255) not null unique,
    nickname   varchar(20)  not null,
    password   varchar(255) not null,
    created_at timestamp   default CURRENT_TIMESTAMP,
    updated_at timestamp   default CURRENT_TIMESTAMP,
    deleted_at timestamp,
    status     varchar(20) default 'ACTIVE'
);

create index idx_users_status_deleted_at on public.users (status, deleted_at);

-- 2. Interests Table
create table public.interests
(
    id               uuid        not null primary key,
    name             varchar(50) not null unique,
    subscriber_count bigint    default 0,
    created_at       timestamp default CURRENT_TIMESTAMP,
    updated_at       timestamp default CURRENT_TIMESTAMP,
    deleted_at       timestamp
);

-- 3. Interest Keywords Table
create table public.interest_keywords
(
    id            uuid        not null primary key,
    interest_id   uuid        not null constraint fk_keyword_interest references public.interests on delete cascade,
    keyword_value varchar(50) not null
);

-- 4. Subscriptions Table
create table public.subscriptions
(
    id          uuid not null primary key,
    user_id     uuid not null constraint fk_sub_user references public.users on delete cascade,
    interest_id uuid not null constraint fk_sub_interest references public.interests on delete cascade,
    created_at  timestamp default CURRENT_TIMESTAMP,
    deleted_at  timestamp,
    updated_at  timestamp,
    constraint uk_user_interest unique (user_id, interest_id)
);

-- 5. Articles Table
create table public.articles
(
    id            uuid         not null primary key,
    source        varchar(100),
    source_url    text         not null unique,
    title         varchar(255) not null,
    summary       text,
    view_count    bigint    default 0,
    comment_count bigint    default 0,
    publish_date  timestamp,
    created_at    timestamp default CURRENT_TIMESTAMP,
    deleted_at    timestamp,
    interest      varchar(255),
    updated_at    timestamp
);

-- 6. Article Views Table (여기에 client_ip 확실히 넣었습니다!)
-- 6. Article Views Table (viewed_at으로 컬럼명 수정 완료)
create table public.article_views
(
    id         uuid not null primary key,
    article_id uuid not null constraint fk_view_article references public.articles on delete cascade,
    viewed_by  uuid not null constraint fk_view_user references public.users on delete cascade,
    client_ip  varchar(45),
    viewed_at  timestamp default CURRENT_TIMESTAMP, -- 엔티티의 @Column(name="viewed_at")과 일치시킴
    constraint uk_article_user_view unique (article_id, viewed_by)
);
-- 7. Comments Table
create table public.comments
(
    id         uuid         not null primary key,
    article_id uuid         not null constraint fk_comment_article references public.articles on delete cascade,
    user_id    uuid         not null constraint fk_comment_user references public.users on delete cascade,
    content    varchar(500) not null,
    like_count bigint    default 0,
    created_at timestamp default CURRENT_TIMESTAMP,
    deleted_at timestamp,
    updated_at timestamp
);

-- 8. Comment Likes Table
create table public.comment_likes
(
    id         uuid not null primary key,
    comment_id uuid not null constraint fk_like_comment references public.comments on delete cascade,
    user_id    uuid not null constraint fk_like_user references public.users on delete cascade,
    created_at timestamp default CURRENT_TIMESTAMP,
    deleted_at timestamp,
    updated_at timestamp,
    constraint uk_comment_user_like unique (comment_id, user_id)
);

-- 9. Notifications Table
create table public.notifications
(
    id            uuid not null primary key,
    user_id       uuid not null constraint fk_noti_user references public.users on delete cascade,
    content       text not null,
    resource_type varchar(50),
    resource_id   uuid,
    is_confirmed  boolean   default false,
    created_at    timestamp default CURRENT_TIMESTAMP,
    updated_at    timestamp default CURRENT_TIMESTAMP,
    deleted_at    timestamp
);