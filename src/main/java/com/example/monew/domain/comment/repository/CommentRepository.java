package com.example.monew.domain.comment.repository;

import com.example.monew.domain.comment.entity.CommentEntity;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<CommentEntity, UUID> , CommentRepositoryCustom{
  @Modifying(clearAutomatically = true)
  @Query(
      value = "DELETE FROM comments "
          + "WHERE user_id = :userId",
      nativeQuery = true
  )
  void deleteAllByUserId(@Param("userId") UUID userId);


  @Modifying(clearAutomatically = true)
  @Query(
      value = "UPDATE comments "
          + "SET deleted_at = :now "
          + "WHERE user_id = :userId "
          + "AND deleted_at IS NULL",
      nativeQuery = true
  )
  void softDeleteAllByUserId(@Param("userId")UUID userId, @Param("now") LocalDateTime now);
}
