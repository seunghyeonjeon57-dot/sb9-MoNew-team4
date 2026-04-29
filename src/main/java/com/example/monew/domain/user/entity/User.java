package com.example.monew.domain.user.entity;

import com.example.monew.domain.user.entity.type.UserStatus;
import com.example.monew.domain.user.exception.NickNameBlankException;
import com.example.monew.global.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;



@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "users")
public class User extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  // 스키마의 varchar(20)에 맞춰 length 추가
  @Column(name = "nickname", nullable = false, length = 20)
  private String nickname;

  @Column(name = "email", nullable = false, unique = true, length = 255)
  private String email;

  @Column(name = "password", nullable = false, length = 255)
  private String password;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private UserStatus status = UserStatus.ACTIVE;

  @Builder
  public User(String nickname, String email, String password, UserStatus status) {
    this.nickname = nickname;
    this.email = email;
    this.password = password;
    this.status = (status != null) ? status : UserStatus.ACTIVE;
  }

  public void updateNickname(String nickname){
    if(nickname==null || nickname.isBlank()){
      throw new NickNameBlankException("닉네임은 공백일 수 없습니다.");
    }
    this.nickname=nickname;
  }
  public void updatePassword(String encodedPassword) {
    this.password = encodedPassword;
  }
  public void withdraw(){
    this.status=UserStatus.DELETED;
    this.markDeleted();
  }

}
