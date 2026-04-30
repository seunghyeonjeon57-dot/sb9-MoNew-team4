package com.example.monew.domain.article.repository;

import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.article.entity.ArticleViewEntity;
import com.example.monew.domain.user.entity.User;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleViewRepository extends JpaRepository<ArticleViewEntity, UUID> {
  void deleteByArticleEntity(ArticleEntity articleEntity);

  boolean existsByArticleEntityIdAndViewedBy(UUID articleId, User user);

  @Query("SELECT av FROM ArticleViewEntity av JOIN FETCH av.articleEntity WHERE av.viewedBy.id = :userId ORDER BY av.viewedAt DESC")
  List<ArticleViewEntity> findTop10ByViewedByIdOrderByViewedAtDesc(@Param("userId") UUID userId);
}
