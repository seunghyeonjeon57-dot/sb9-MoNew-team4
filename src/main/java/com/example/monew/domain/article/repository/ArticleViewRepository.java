package com.example.monew.domain.article.repository;

import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.article.entity.ArticleViewEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleViewRepository extends JpaRepository<ArticleViewEntity, UUID> {
  void deleteByArticleEntity(ArticleEntity articleEntity);

  boolean existsByArticleEntityIdAndViewedBy(UUID articleId, UUID viewedBy);
}
