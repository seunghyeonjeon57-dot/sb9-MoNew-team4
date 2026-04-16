package com.example.monew.domain.article.repository;

import com.example.monew.domain.article.entity.ArticleEntity;

import java.time.LocalDateTime;
import java.util.List;

public interface ArticleRepositoryCustom {

  List<ArticleEntity> findByCursor(Long cursor, LocalDateTime after, int size);
}