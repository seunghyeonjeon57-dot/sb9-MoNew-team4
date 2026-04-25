package com.example.monew.domain.user.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import com.example.monew.domain.user.exception.NickNameBlankException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


class UserTest {
  @Test
  @DisplayName("닉네임 수정 성공")
  void updateName_success(){
    User user  = User.builder().nickname("이전 닉네임").build();
    user.updateNickname("새 닉네임");
    assertThat(user.getNickname()).isEqualTo("새 닉네임");
  }
  @Test
  @DisplayName("닉네임이 공백이면 예외가 발생함")
  void UpdateNickname_fail_black(){
    User user = User.builder().nickname("원래 닉네임").build();
    assertThatThrownBy(()->user.updateNickname(" "))
        .isInstanceOf(NickNameBlankException.class)
        .hasMessage("닉네임은 공백일 수 없습니다.");

  }

}