package com.example.monew.domain.article.repository;

import com.example.monew.domain.article.dto.ArticleSearchCondition;
import com.example.monew.domain.article.entity.ArticleEntity;

import java.util.List;
import java.util.UUID;

public interface ArticleRepositoryCustom {

  long softDelete(UUID id);

  List<ArticleEntity> findAllActive();

  List<ArticleEntity> findByCursor(ArticleSearchCondition condition);
}