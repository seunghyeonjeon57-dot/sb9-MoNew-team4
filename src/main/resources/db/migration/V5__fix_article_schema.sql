-- 1. 부모(articles)의 PK 구조를 변경하기 위해 자식 테이블들의 외래 키를 잠시 제거합니다.
ALTER TABLE public.article_views DROP CONSTRAINT IF EXISTS fk_view_article;
ALTER TABLE public.comments DROP CONSTRAINT IF EXISTS fk_comment_article;

-- 2. articles 테이블에 혹시라도 남아있을지 모르는 중복 컬럼(article_id)을 삭제하여 혼선을 방지합니다.
ALTER TABLE public.articles DROP COLUMN IF EXISTS article_id;

-- 3. [핵심] 기존 PK 제약 조건을 삭제한 후, id 컬럼을 PK로 재설정합니다.
-- H2(로컬/CI)와 PostgreSQL(운영)의 PK 명칭 차이를 모두 고려하여 DROP을 시도합니다.
ALTER TABLE public.articles DROP CONSTRAINT IF EXISTS articles_pkey CASCADE;
ALTER TABLE public.articles DROP CONSTRAINT IF EXISTS PRIMARY_KEY_8;
ALTER TABLE public.articles ADD PRIMARY KEY (id);

-- 4. 제거했던 자식 테이블들의 외래 키를 다시 복구합니다. (ON DELETE CASCADE 유지)
ALTER TABLE public.article_views
    ADD CONSTRAINT fk_view_article
        FOREIGN KEY (article_id) REFERENCES public.articles(id) ON DELETE CASCADE;

ALTER TABLE public.comments
    ADD CONSTRAINT fk_comment_article
        FOREIGN KEY (article_id) REFERENCES public.articles(id) ON DELETE CASCADE;