package com.example.monew.domain.comment.mapper;

import com.example.monew.domain.comment.dto.CommentDto;
import com.example.monew.domain.comment.entity.CommentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CommentMapper {
  @Mapping(target = "userNickname", source = "nickname")
  @Mapping(target = "likedByMe", source = "liked")
  CommentDto toDto(CommentEntity entity, String nickname, boolean liked);
}
