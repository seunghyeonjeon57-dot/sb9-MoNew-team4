package com.example.monew.domain.interest.entity;

import com.example.monew.global.base.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

@Entity
@Table(name = "interests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Interest extends BaseEntity {

  @Id
  @Column(columnDefinition = "uuid")
  private UUID id = UUID.randomUUID();

  @Column(nullable = false, unique = true)
  private String name;

  @OneToMany(mappedBy = "interest", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<InterestKeyword> keywords = new ArrayList<>();

  @Column(nullable = false)
  private long subscriberCount = 0L;

  public Interest(String name, List<String> keywords) {
    if (!StringUtils.hasText(name)) {
      throw new IllegalArgumentException("관심사 이름은 비어 있을 수 없습니다.");
    }
    if (keywords == null || keywords.isEmpty()) {
      throw new IllegalArgumentException("키워드는 최소 1개 이상이어야 합니다.");
    }
    this.name = name;
    replaceKeywords(keywords);
  }

  public void replaceKeywords(List<String> newKeywords) {
    this.keywords.clear();
    for (String value : newKeywords) {
      this.keywords.add(new InterestKeyword(this, value));
    }
  }

}
