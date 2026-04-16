package com.example.monew.domain.article.repository;

import com.example.monew.domain.article.entity.ArticleEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ArticleRepositoryCustom {

  List<ArticleEntity> findByCursor(UUID cursor, LocalDateTime after, int size);
}