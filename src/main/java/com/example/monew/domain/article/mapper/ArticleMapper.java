package com.example.monew.domain.article.mapper;


import com.example.monew.domain.article.dto.ArticleDto;
import com.example.monew.domain.article.dto.CursorPageResponseArticleDto;
import com.example.monew.domain.article.entity.ArticleEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring",unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ArticleMapper {


  @Mapping(target = "viewedByMe", source = "viewedByMe")
  ArticleDto toDto(ArticleEntity entity, boolean viewedByMe);
}