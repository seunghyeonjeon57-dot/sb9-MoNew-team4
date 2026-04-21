package com.example.monew.domain.comment.repository;

import com.example.monew.domain.comment.entity.CommentLikeEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLikeEntity, UUID> {
  boolean existsByCommentIdAndUserId(UUID commentId, UUID userId);

  void deleteByCommentIdAndUserId(UUID commentId, UUID UserId);

  @Modifying(clearAutomatically = true)
  @Query("delete from CommentLikeEntity c where c.userId = :userId")
  void deleteAllByUserId(@Param("userId") UUID userId);
}
