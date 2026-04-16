package com.example.monew.domain.article.mapper;


import com.example.monew.domain.article.dto.ArticleDto;
import com.example.monew.domain.article.entity.ArticleEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface ArticleMapper {


  @Mapping(target = "viewedByMe", source = "viewedByMe")
  ArticleDto toDto(ArticleEntity entity, boolean viewedByMe);
}