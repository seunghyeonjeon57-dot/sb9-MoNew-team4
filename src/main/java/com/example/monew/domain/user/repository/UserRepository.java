package com.example.monew.domain.user.repository;

import com.example.monew.domain.user.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
  boolean existsByEmail(String email);
  Optional<User> findByEmail(String email);

  @Query(value = "SELECT * FROM users WHERE deleted_at <= :threshold", nativeQuery = true)
  List<User> findExpiredDeletedUsers(@Param("threshold") LocalDateTime threshold);

  
  @Modifying
  @Query(value = "DELETE FROM users WHERE id = :id", nativeQuery = true)
  void hardDeleteById(@Param("id") UUID id);

}
