package com.example.monew.domain.comment.repository;

import com.example.monew.domain.comment.entity.CommentEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<CommentEntity, UUID> , CommentRepositoryCustom{

}
