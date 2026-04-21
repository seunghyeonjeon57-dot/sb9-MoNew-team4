package com.example.monew.domain.article.repository;

import com.example.monew.domain.article.entity.ArticleViewEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleViewRepository extends JpaRepository<ArticleViewEntity, UUID> {

}
