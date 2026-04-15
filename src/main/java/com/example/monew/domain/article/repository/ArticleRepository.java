package com.example.monew.domain.article.repository;

import com.example.monew.domain.article.entity.ArticleEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleRepository extends JpaRepository<ArticleEntity, UUID> {

  @Query("SELECT a FROM ArticleEntity a WHERE " +
      "(:keyword IS NULL OR a.title LIKE %:keyword% OR a.summary LIKE %:keyword%) AND " +

      "(:interest IS NULL OR a.interest = :interest) AND " +
      "(:source IS NULL OR a.source = :source) AND " +

      "(:start IS NULL OR a.publishDate >= :start) AND " +
      "(:end IS NULL OR a.publishDate <= :end)")

  Page<ArticleEntity> searchArticles(
      @Param("keyword") String keyword,
      @Param("interest") String interest,
      @Param("source") String source,

      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end,
      Pageable pageable);

  @Query("SELECT DISTINCT a.source FROM ArticleEntity a")
  List<String> findAllSources();

  @Modifying
  @Query(value = "DELETE FROM articles WHERE article_id = :id", nativeQuery = true)
  void hardDeleteById(@Param("id") UUID id);

  @Modifying
  @Query(value = "UPDATE articles SET is_deleted = false WHERE article_id = :id", nativeQuery = true)
  void restoreById(@Param("id") UUID id);

  boolean existsBySourceUrl(String sourceUrl);
}
