package com.example.monew.domain.user.controller;

import com.example.monew.domain.user.dto.UserDto;
import com.example.monew.domain.user.dto.request.UserLoginRequest;
import com.example.monew.domain.user.dto.request.UserRegisterRequest;
import com.example.monew.domain.user.dto.request.UserUpdateRequest;
import com.example.monew.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name="사용자 관리",description = "사용자 관련 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;

  @Operation(summary = "회원가입",description = "새로운 사용자를 등록합니다")
  @ApiResponse(responseCode = "201", description = "회원가입 성공")
  @ApiResponse(responseCode = "400", description = "잘못된 요청(입력값 검증 실패)")
  @ApiResponse(responseCode = "409", description = "이메일 중복")
  @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  @PostMapping
  public ResponseEntity<Void> create(@Valid @RequestBody UserRegisterRequest request){
    userService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }
  @Operation(summary = "로그인", description = "사용자 로그인을 처리합니다.")
  @ApiResponse(responseCode = "200",description = "로그인 성공")
  @ApiResponse(responseCode = "400",description = "잘못된 요청(입력값 검증 실패)")
  @ApiResponse(responseCode = "401",description = "로그인 실패 (이메일 또는 비밀번호 불일치)")
  @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  @PostMapping("/login")
  public ResponseEntity<UserDto> login(@Valid @RequestBody UserLoginRequest request){
    return ResponseEntity.ok(userService.login(request));
  }
  @Operation(summary = "사용자 논리 삭제", description = "사용자를 논리적으로 삭제합니다.")
  @ApiResponse(responseCode = "204",description = "사용자 삭제 성공")
  @ApiResponse(responseCode = "403",description = "사용자 삭제 권한 없음")
  @ApiResponse(responseCode = "404",description = "사용자 정보 없음")
  @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  @DeleteMapping("/{userId}")
  public ResponseEntity<Void> softDeleteUser(@PathVariable UUID userId){
    userService.softDeleteUser(userId);
    return ResponseEntity.noContent().build();
  }
  @Operation(summary = "사용자 정보 수정", description = "사용자 닉네임을 수정합니다.")
  @ApiResponse(responseCode = "200",description = "사용자 정보 수정 성공")
  @ApiResponse(responseCode = "400",description = "잘못된 요청(입력값 검증 실패)")
  @ApiResponse(responseCode = "403",description = "사용자 정보 수정 권한 없음")
  @ApiResponse(responseCode = "404", description = "사용자 정보 없음")
  @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  @PatchMapping("/{userId}")
  public ResponseEntity<UserDto> update( @PathVariable UUID userId,
     @Valid @RequestBody UserUpdateRequest request){
    return ResponseEntity.ok(userService.updateUser(userId,request));
  }


  @Operation(summary = "사용자 물리 삭제", description = "사용자를 물리적으로 삭제합니다.")
  @ApiResponse(responseCode = "204",description = "사용자 삭제 성공")
  @ApiResponse(responseCode = "403",description = "사용자 삭제 권한 없음")
  @ApiResponse(responseCode = "404",description = "사용자 정보 없음")
  @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  @DeleteMapping("/{userId}/hard")
  public ResponseEntity<Void> hardDelete(@PathVariable UUID userId) {
    userService.hardDeleteUser(userId);
    return ResponseEntity.noContent().build();
  }



}
