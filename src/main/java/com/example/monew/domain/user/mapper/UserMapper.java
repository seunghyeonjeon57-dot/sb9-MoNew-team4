package com.example.monew.domain.user.mapper;


import com.example.monew.domain.user.dto.UserDto;
import com.example.monew.domain.user.dto.request.UserRegisterRequest;
import com.example.monew.domain.user.entity.User;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE
,builder = @Builder(disableBuilder = true))
public interface UserMapper {
  User toEntity(UserRegisterRequest request);

  UserDto toDto(User user);



}
