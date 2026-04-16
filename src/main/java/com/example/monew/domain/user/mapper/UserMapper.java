package com.example.monew.domain.user.mapper;


import com.example.monew.domain.user.dto.request.UserRegisterRequest;
import com.example.monew.domain.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
  @Mapping(target = "id" , ignore = true)
  @Mapping(target= "createdAt", ignore = true)
  User toEntity(UserRegisterRequest request);



}
