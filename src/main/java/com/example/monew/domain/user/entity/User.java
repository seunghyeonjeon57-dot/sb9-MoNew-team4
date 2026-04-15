package com.example.monew.domain.user.entity;

import com.example.monew.global.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.Where;


@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "users")
@SoftDelete(columnName = "deleted_at")
public class User extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;
  @Column(nullable = false)
  private String nickname;
  @Column(nullable = false,unique = true)
  private String email;
  @Column(nullable = false)
  private String password;

  @Builder
  public User(String nickname,String email,String password){
    this.nickname=nickname;
    this.email=email;
    this.password=password;
  }

  public void updateNickname(String nickname){
    if(nickname==null || nickname.isBlank()){
      throw new IllegalArgumentException("닉네임은 공백일 수 없습니다.");
    }
    this.nickname=nickname;
  }

}
