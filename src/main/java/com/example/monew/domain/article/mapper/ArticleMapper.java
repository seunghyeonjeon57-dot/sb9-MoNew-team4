package com.example.monew.domain.article.mapper;


import com.example.monew.domain.article.dto.ArticleDto;
import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.interest.entity.InterestKeyword;
import java.util.List;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;


@Mapper(componentModel = "spring")
public interface ArticleMapper {

  @Mapping(target = "viewedByMe", source = "viewedByMe")
  @Mapping(target = "keywords", source = "entity.interest.keywords", qualifiedByName = "mapKeywords")

  ArticleDto toDto(ArticleEntity entity, boolean viewedByMe);


  @Named("mapKeywords")
  default List<String> mapKeywords(List<InterestKeyword> keywords) {
    if (keywords == null) {
      return List.of();
    }
    return keywords.stream()
        .map(InterestKeyword::getValue)
        .collect(Collectors.toList());
  }
}