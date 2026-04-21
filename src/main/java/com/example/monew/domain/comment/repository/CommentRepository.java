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
  void deleteAllByUserId(UUID userId);

  @Modifying(clearAutomatically = true)
  @Query("UPDATE CommentEntity c "
      + "SET c.deletedAt = :now "
      + "WHERE c.userId = :userId "
      + "AND c.deletedAt IS NULL")
  void softDeleteAllByUserId(@Param("userId")UUID userId, @Param("now") LocalDateTime now);
}
