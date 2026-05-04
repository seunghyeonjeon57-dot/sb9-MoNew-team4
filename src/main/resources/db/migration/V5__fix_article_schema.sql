-- 1. 외래 키 제약 조건을 잠시 제거 (컬럼 삭제 및 수정을 위해)
ALTER TABLE public.article_views DROP CONSTRAINT IF EXISTS fk_view_article;
ALTER TABLE public.comments DROP CONSTRAINT IF EXISTS fk_comment_article;

-- 2. [핵심] articles 테이블의 유령 컬럼(article_id)만 삭제
-- 이미 id가 PK이므로 이 작업만으로도 충분합니다.
ALTER TABLE public.articles DROP COLUMN IF EXISTS article_id;

-- 3. 자식 테이블들의 외래 키를 다시 연결
-- articles(id)를 참조하도록 다시 꽉 묶어줍니다.
ALTER TABLE public.article_views
    ADD CONSTRAINT fk_view_article
        FOREIGN KEY (article_id) REFERENCES public.articles(id) ON DELETE CASCADE;

ALTER TABLE public.comments
    ADD CONSTRAINT fk_comment_article
        FOREIGN KEY (article_id) REFERENCES public.articles(id) ON DELETE CASCADE;