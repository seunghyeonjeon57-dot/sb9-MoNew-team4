-- 1. 부모(articles)의 PK를 건드리기 위해 자식들의 FK 제약 조건을 먼저 제거합니다.
-- V1에서 정의한 제약 조건 이름(fk_view_article, fk_comment_article)을 그대로 사용합니다.
ALTER TABLE public.article_views DROP CONSTRAINT IF EXISTS fk_view_article;
ALTER TABLE public.comments DROP CONSTRAINT IF EXISTS fk_comment_article;

-- 2. articles 테이블에 혹시라도 남아있을지 모르는 중복 컬럼 'article_id'를 삭제합니다.
-- V1에서는 id가 PK이므로, 명칭이 혼용되는 것을 막기 위해 유령 컬럼을 밀어버립니다.
ALTER TABLE public.articles DROP COLUMN IF EXISTS article_id;

-- 3. articles 테이블의 PK가 'id'임을 확실히 재선언합니다.
ALTER TABLE public.articles DROP CONSTRAINT IF EXISTS articles_pkey CASCADE;
ALTER TABLE public.articles ADD PRIMARY KEY (id);

-- 4. 자식 테이블들의 외래 키를 V1의 설정(ON DELETE CASCADE) 그대로 다시 복구합니다.
-- 자식들의 컬럼명은 V1과 동일하게 'article_id'를 유지하며 부모의 'id'를 참조합니다.
ALTER TABLE public.article_views
    ADD CONSTRAINT fk_view_article
        FOREIGN KEY (article_id) REFERENCES public.articles(id) ON DELETE CASCADE;

ALTER TABLE public.comments
    ADD CONSTRAINT fk_comment_article
        FOREIGN KEY (article_id) REFERENCES public.articles(id) ON DELETE CASCADE;