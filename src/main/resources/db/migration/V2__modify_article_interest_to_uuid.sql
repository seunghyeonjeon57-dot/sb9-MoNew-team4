-- 1. V1에서 잘못 생성된 varchar(255) 타입의 interest 컬럼 삭제
ALTER TABLE public.articles DROP COLUMN IF EXISTS interest;

-- 2. Interests 테이블의 ID(UUID)를 참조할 정석적인 컬럼 추가
ALTER TABLE public.articles ADD COLUMN interest_id uuid;

-- 3. 두 테이블을 논리적/물리적으로 연결하는 외래 키(FK) 제약 조건 설정
ALTER TABLE public.articles
    ADD CONSTRAINT fk_article_interest
        FOREIGN KEY (interest_id) REFERENCES public.interests(id)
            ON DELETE SET NULL;