package com.example.monew.domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRegisterRequest(
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @NotBlank(message = "이메일은 필수 입력값입니다.")
    String email,
    @NotBlank(message = "닉네임은 필수 입력값입니다.")
    @Size(min = 1,max = 20,message = "닉네임은 1자 이상 20자 이하로 입력해주세요.")
    String nickname,
    @NotBlank(message = "비밀번호는 필수 입력값입니다.")
    @Size(min = 6, max = 20 , message = "비밀번호는 6자 이상 20자 이하로 입력해주세요.")
    String password

) {

}
