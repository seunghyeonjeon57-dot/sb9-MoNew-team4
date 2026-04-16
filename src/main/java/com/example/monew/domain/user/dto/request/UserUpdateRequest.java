package com.example.monew.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
    @NotBlank(message = "닉네임은 필수입력값입니다.")
    @Size(min =1,max=20,message = "닉네임은 1자 이상 20자 이하로 입력해야합니다.")
    String nickname
) {

}
